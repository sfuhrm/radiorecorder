/*
 * Copyright 2017 Stephan Fuhrmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sfuhrm.radiorecorder.consumer;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.Main;
import de.sfuhrm.radiorecorder.RadioException;
import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Copies a stream to one or multiple disk files.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamCopyConsumer extends MetaDataConsumer implements Consumer<HttpConnection> {

    /** When the consumer was created. */
    private final long creationTimeStamp;

    /**
     * The consecutive file number.
     */
    private int fileNumber;

    /**
     * The last metadata received. Can be {@code null}.
     */
    private MetaData metaData;

    /**
     * The last metadata received. Can be {@code null}.
     */
    private MetaData previousMetaData;

    /**
     * Whether the metadata changed in the meantime. Indicates that a new file
     * needs to be opened.
     */
    private boolean metaDataChanged;

    /** The directory to write files to. */
    private final File directory;

    /**
     * The current file being written to, if any.
     *
     * @see #outputStream
     */
    private Optional<File> file = Optional.empty();

    /**
     * The current output stream being written to, if any.
     *
     * @see #file
     */
    private Optional<FileOutputStream> outputStream = Optional.empty();

    /** Constructor.
     * @param consumerContext the context to work in.
     * */
    public StreamCopyConsumer(ConsumerContext consumerContext) {
        super(consumerContext);

        creationTimeStamp = System.currentTimeMillis();
        directory = createDirectory(consumerContext);
        try {
            initFileNumber();
        } catch (IOException ex) {
            log.warn("File number finding problem", ex);
            fileNumber = 1;
        }
    }

    private File createDirectory(ConsumerContext context) {
        File parent = context.getTargetDirectory();
        String hostAndPath;
        try {
            hostAndPath = URLEncoder.encode(context.getRadio().getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        File dir = new File(parent, hostAndPath);
        dir.mkdirs();
        return dir;
    }

    /**
     * Check whether aborting is necessary because of restrictions to
     * file system or maximum write size.
     *
     * @see ConsumerContext#getMinFree()
     * @see ConsumerContext#getAbortAfterFileLength()
     */
    private boolean needToAbort(Optional<File> currentFile) throws IOException {
        if (currentFile.isPresent()) {
            File f = currentFile.get();

            if (getContext().getAbortAfterFileLength().isPresent()) {
                if (f.length() > getContext().getAbortAfterFileLength().get()) {
                    log.warn("Aborting due to maximum file size of {} exceeded: {} file size, {} is the abort-after size",
                            f,
                            f.length(),
                            getContext().getAbortAfterFileLength().get());
                    return true;
                }
            }

            if (getContext().getAbortAfterDuration().isPresent()) {
                long abortAfterMillis = getContext().getAbortAfterDuration().get();
                long elapsedMillis = System.currentTimeMillis() - creationTimeStamp;
                if (elapsedMillis > abortAfterMillis) {
                    log.warn("Aborting due to maximum duration of {}ms",
                            System.currentTimeMillis() - creationTimeStamp);
                    return true;
                }
            }

            FileStore fileStore = Files.getFileStore(f.toPath());
            long free = fileStore.getUsableSpace();
            long required = getContext().getMinFree();
            if (free < required) {
                log.warn("Path {} is too full, has less than {} bytes free", f, required);
                return true;
            }
        }
        return false;
    }

    private void openUnnamedFileAndInputStream(Optional<MimeType> contentType) {
        File f = getNumberFile(contentType);
        file = Optional.of(f);
        try {
            outputStream = Optional.of(new FileOutputStream(f));
        } catch (FileNotFoundException ex) {
            throw new RadioException(false, ex);
        }
        log.debug("Copying from url {} to file {}, type {}",
                getContext().getUrl().toExternalForm(),
                f,
                contentType);
    }

    private void closeOldFileAndReopenWithNewMetadata(Optional<MimeType> contentType) throws IOException {
        log.debug("Meta data changed");
        metaDataChanged = false;
        closeStreamIfOpen(outputStream, file, contentType);

        file = getFileFromMetaData(contentType);
        log.debug("New file {}", file);
        outputStream = file.isPresent()
                ? Optional.of(new FileOutputStream(file.get()))
                : Optional.empty();
    }

    @Override
    protected void __accept(HttpConnection t, InputStream inputStream) {
        Runnable cleanup = () -> cleanup(getContext().isSongNames());
        Thread cleanupThread = new Thread(cleanup);
        Runtime.getRuntime().addShutdownHook(cleanupThread);
        try {
            getStreamMetaData().setMetaDataConsumer(m -> {
                this.previousMetaData = metaData;
                this.metaData = m;
                metaDataChanged = true;
                new ConsoleMetaDataConsumer().accept(m);
            });
            byte[] buffer = new byte[BUFFER_SIZE];
            Optional<MimeType> contentType = MimeType.byContentType(t.getContentType());

            if (!getContext().isSongNames()) {
                openUnnamedFileAndInputStream(contentType);
            }

            int len;
            long ofs = 0;
            while (-1 != (len = inputStream.read(buffer))) {
                try {
                    if (needToAbort(file)) {
                        return;
                    }
                    if (metaDataChanged && getContext().isSongNames()) {
                        closeOldFileAndReopenWithNewMetadata(contentType);
                    }

                    if (outputStream.isPresent()) {
                        outputStream.get().write(buffer, 0, len);
                    } else {
                        log.info("Dropped {} bytes, no file name yet", len);
                    }
                } catch (IOException ioe) {
                    throw new RadioException(false, ioe);
                }
                ofs += len;
                log.trace("Copied {} bytes", ofs);
            }

        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            fileNumber++;
            throw new RadioException(true, ex);
        } finally {
            cleanup(getContext().isSongNames());
            Runtime.getRuntime().removeShutdownHook(cleanupThread);
        }
    }

    private void cleanup(boolean deletePartly) {
        if (outputStream.isPresent()) {
            try {
                outputStream.get().close();
            } catch (IOException ex) {
                log.warn("URL " + getContext().getUrl().toExternalForm() + " close error", ex);
            }
        }
        if (file.isPresent() && deletePartly) {
            log.info("Deleting partly file {}", file.get());
            file.get().delete();
        }
    }

    private void closeStreamIfOpen(Optional<FileOutputStream> outputStream, Optional<File> file, Optional<MimeType> contentType) throws IOException {
        MetaData fileMetaData = previousMetaData != null ? previousMetaData.clone() : null;
        if (outputStream.isPresent()) {
            log.debug("Closing output stream to {}", file.orElse(null));
            outputStream.get().close();

            if (contentType.isPresent() && contentType.get() == MimeType.AUDIO_MPEG && file.isPresent() && fileMetaData != null) {
                Runnable r = () -> {
                    try {
                        addID3Tags(fileMetaData, file.get());
                    } catch (IOException ex) {
                        log.warn("Error while adding id3 tags", ex);
                    }
                };
                Thread t = new Thread(r, "ID3 " + file.get());
                t.start();
            }

            // adjust time to stream start
            if (file.isPresent() && fileMetaData != null) {
                File f = file.get();
                Files.setLastModifiedTime(f.toPath(), FileTime.from(fileMetaData.getCreated().toInstant()));
            }
        }
    }

    private void addID3Tags(final MetaData md, final File file) throws IOException {
        try {
            log.debug("Adding id3 tag to {}", file);
            Mp3File mp3File = new Mp3File(file);

            ID3v1 id3v1 = new ID3v1Tag();
            md.getTitle().ifPresent(id3v1::setTitle);
            md.getArtist().ifPresent(id3v1::setArtist);
            md.getStationName().ifPresent(id3v1::setComment);
            mp3File.setId3v1Tag(id3v1);

            ID3v24Tag id3v2 = new ID3v24Tag();
            md.getTitle().ifPresent(id3v2::setTitle);
            md.getArtist().ifPresent(id3v2::setArtist);
            md.getStationName().ifPresent(id3v2::setPublisher);
            md.getStationUrl().ifPresent(id3v2::setRadiostationUrl);
            id3v2.setComment(Main.PROJECT);
            id3v2.setUrl(Main.GITHUB_URL);
            mp3File.setId3v2Tag(id3v2);

            File bak = new File(file.getParentFile(), file.getName() + ".bak");
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            mp3File.save(tmp.getAbsolutePath());

            // foo.mp3 -> foo.mp3.bak
            Files.move(file.toPath(), bak.toPath());
            // foo.mp3.tmp -> foo.mp3
            Files.move(tmp.toPath(), file.toPath());
            // foo.mp3.bak
            Files.delete(bak.toPath());

            log.debug("Done adding id3 tag to {}", file);
        } catch (NotSupportedException | UnsupportedTagException | InvalidDataException ex) {
            log.warn("Exception while writing id3 tag for " + file, ex);
        }
    }

    /** Find the maximum used file number on disk. */
    private void initFileNumber() throws IOException {
        final Pattern integerPattern = Pattern.compile("[0-9]+");

        // this works for both songname files and number files
        try (final Stream<Path> stream = Files.list(directory.toPath())) {
            OptionalInt maxFileNumber = stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.contains("."))
                    .map(s -> s.substring(0, s.indexOf('.')))
                    .filter(s -> integerPattern.matcher(s).matches())
                    .mapToInt(Integer::parseInt)
                    .max();
            fileNumber = maxFileNumber.orElse(0) + 1;
            log.debug("Found file number {}, fileNumber starts at {}", maxFileNumber, fileNumber);
        }
    }

    /**
     * Get the next number based filename.
     *
     * @param contentType content type for calculating the suffix.
     */
    private File getNumberFile(Optional<MimeType> contentType) {
        File f;
        do {
            String fileName = String.format("%03d%s", fileNumber,
                    suffixFromContentType(contentType));
            f = new File(directory, fileName);
            fileNumber++;
        } while (f.exists() && f.length() != 0);
        return f;
    }

    /**
     * Get the file name derived from the received metadata.
     *
     * @return the file, if there is metadata, or empty.
     */
    private Optional<File> getFileFromMetaData(Optional<MimeType> contentType) {
        Optional<File> result = Optional.empty();
        if (metaData != null) {
            File file;
            do {
                String unknown = "unknown";
                String fileName = String.format("%03d.%s - %s%s", fileNumber++,
                        sanitizeFileName(metaData.getArtist().orElse(unknown)),
                        sanitizeFileName(metaData.getTitle().orElse(unknown)),
                        suffixFromContentType(contentType));
                file = new File(directory, fileName);
            } while (file.exists() && file.length() != 0);
            result = Optional.of(file);
        }
        return result;
    }

    private static String sanitizeFileName(String in) {
        return in.replaceAll("[/:\\|]", "_");
    }

    /**
     * Calculate the file suffix.
     */
    private static String suffixFromContentType(Optional<MimeType> contentType) {
        if (!contentType.isPresent()) {
            return "";
        }
        return contentType.get().getSuffix();
    }
}
