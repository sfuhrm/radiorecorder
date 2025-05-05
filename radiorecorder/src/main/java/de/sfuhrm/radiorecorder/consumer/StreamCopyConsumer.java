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

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.RadioException;
import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    /** The directory to write files to.
     * @see #fileNameGeneratorSupplier
     * */
    private final Path targetDirectory;

    /**
     * The current file being written to, if any.
     *
     * @see #outputStreamNullable
     */
    private Path fileNullable = null;

    /**
     * The current output stream being written to, if any.
     *
     * @see #fileNullable
     */
    private OutputStream outputStreamNullable = null;

    /**
     * Helps with file names ;).
     */
    private final Supplier<MetaDataFileNameGenerator> fileNameGeneratorSupplier;

    /** Constructor.
     * @param consumerContext the context to work in.
     * */
    public StreamCopyConsumer(ConsumerContext consumerContext) {
        super(consumerContext);

        creationTimeStamp = System.currentTimeMillis();

        fileNameGeneratorSupplier = () -> useSongNames()
                ? new MetaDataFileNameGenerator(consumerContext.getSongnameFormat(), consumerContext, true) :
                  new MetaDataFileNameGenerator(consumerContext.getNoSongnameFormat(), consumerContext, false);

        targetDirectory = consumerContext.getTargetDirectory();
    }

    /** Returns if the stream has metadata and we are processing songnames. */
    private boolean useSongNames() {
        return getStreamMetaData().isProvidesMetaData() && getContext().isSongNames();
    }
    
    /**
     * Check whether aborting is necessary because of restrictions to
     * file system or maximum write size.
     *
     * @see ConsumerContext#getMinFree()
     * @see ConsumerContext#getAbortAfterFileLength()
     */
    private boolean needToAbort(Path currentFileOrNull) throws IOException {
        if (currentFileOrNull != null) {
            long length = Files.size(currentFileOrNull);
            if (getContext().getAbortAfterFileLength().isPresent()) {
                if (length > getContext().getAbortAfterFileLength().get()) {
                    log.warn("Aborting due to maximum file size of {} exceeded: {} file size, {} is the abort-after size",
                            currentFileOrNull,
                            length,
                            getContext().getAbortAfterFileLength().get());
                    return true;
                }
            }
        }

        FileStore fileStore = Files.getFileStore(targetDirectory);
        long free = fileStore.getUsableSpace();
        long required = getContext().getMinFree();
        if (free < required) {
            log.warn("Path {} is too full, has less than {} bytes free", targetDirectory, required);
            return true;
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

        return false;
    }

    private void closeOldFileAndReopenWithNewMetadata(MimeType contentTypeOrNull) throws IOException {
        log.debug("Meta data changed");
        metaDataChanged = false;
        closeStreamIfOpen(outputStreamNullable, fileNullable, contentTypeOrNull);

        Optional<Path> optionalPath = fileNameGeneratorSupplier.get().getFileFrom(getContext().getRadio(), metaData, contentTypeOrNull);
        if (optionalPath.isPresent()) {
            fileNullable = optionalPath.get();
            ensureParentDirectoriesExist(fileNullable);
            outputStreamNullable = Files.newOutputStream(fileNullable, StandardOpenOption.CREATE_NEW);
        } else {
            fileNullable = null;
            outputStreamNullable = null;
        }
        log.debug("New file {}", fileNullable);
    }

    @Override
    protected void __accept(HttpConnection t, InputStream inputStream) {
        Runnable cleanup = () -> cleanup(useSongNames());
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

            // open stream in case no songname is existing yet
            if (!useSongNames()) {
                closeOldFileAndReopenWithNewMetadata(contentType.orElse(null));
            }

            int len;
            long ofs = 0;
            boolean dropMsgWritten = false;
            while (-1 != (len = inputStream.read(buffer))) {
                try {
                    if (needToAbort(fileNullable)) {
                        return;
                    }

                    // open new output stream if metadata has changed, we're using song names, and
                    // we're not in the first (incomplete) song (see #37)
                    if (metaDataChanged && useSongNames() && metaData.getIndex().orElse(0) > 0) {
                        closeOldFileAndReopenWithNewMetadata(contentType.orElse(null));
                    }

                    if (outputStreamNullable != null) {
                        outputStreamNullable.write(buffer, 0, len);
                    } else {
                        if (!dropMsgWritten) {
                            log.info("Dropping bytes of incomplete file, waiting for next song");
                            dropMsgWritten = true;
                        }
                    }
                } catch (IOException ioe) {
                    throw new RadioException(false, ioe);
                }
                ofs += len;
                log.trace("Copied {} bytes", ofs);
            }

        } catch (IOException ex) {
            log.warn("URL {} broke down", getContext().getUri().toASCIIString(), ex);
            fileNumber++;
            throw new RadioException(true, ex);
        } finally {
            cleanup(useSongNames());
            Runtime.getRuntime().removeShutdownHook(cleanupThread);
        }
    }

    private void cleanup(boolean deletePartly) {
        if (outputStreamNullable != null) {
            try {
                outputStreamNullable.close();
            } catch (IOException ex) {
                log.warn("URL {} close error", getContext().getUri().toASCIIString(), ex);
            }
        }
        if (fileNullable != null && deletePartly && Files.exists(fileNullable)) {
            log.info("Deleting partly file {}", fileNullable);
            try {
                Files.delete(fileNullable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void closeStreamIfOpen(OutputStream outputStreamOrNull, Path fileOrNull, MimeType contentTypeOrNull) throws IOException {
        MetaData fileMetaData = previousMetaData != null ? previousMetaData.clone() : null;
        if (outputStreamOrNull != null) {
            log.debug("Closing output stream to {}", fileOrNull);
            outputStreamOrNull.close();

            Runnable idtagger;
            if (contentTypeOrNull == MimeType.AUDIO_MPEG
                    && fileOrNull != null
                    && fileMetaData != null) {
                idtagger = new ID3Tagger(fileMetaData, fileOrNull);
            } else {
                idtagger = null;
            }

            Runnable postprocess = () -> {
                try {
                    // adjust time to stream start
                    if (fileOrNull != null && fileMetaData != null) {
                        Files.setLastModifiedTime(fileOrNull, FileTime.from(fileMetaData.getCreated().toInstant()));
                    }
                } catch (IOException e) {
                    log.warn("Error setting date for {}", fileOrNull, e);
                }
            };

            // set time synchronously
            // tag id3 asynchronously
            if (idtagger != null) {
                Thread postprocessThread = new Thread(() -> {
                    idtagger.run();
                    postprocess.run();
                }, "ID3 " + fileOrNull);
                postprocessThread.start();
            } else {
                postprocess.run();
            }

        }
    }

    private void ensureParentDirectoriesExist(Path p) throws IOException {
        Path parent = p.getParent();
        if (! Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
