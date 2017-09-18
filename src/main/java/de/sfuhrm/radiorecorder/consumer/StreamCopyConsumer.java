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
import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/** Copies a stream to one or multiple disk files.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamCopyConsumer extends MetaDataConsumer implements Consumer<URLConnection> {

    /** The consecutive file number. */
    private int fileNumber;

    /** The last meta data received. Can be {@code null}. */
    private MetaData metaData;
    
    /** The last meta data received. Can be {@code null}. */
    private MetaData previousMetaData;
    
    /** Whether the meta data changed in the meantime. Indicates that a new file
     * needs to be opened.
     */
    private boolean metaDataChanged;

    public StreamCopyConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        fileNumber = 1;        
    }
    
    /** Check whether aborting is necessary because of full file system.
     * @see ConsumerContext#getMinFree()  
     */
    private boolean needToAbort(Optional<File> currentFile) throws IOException {
        if (currentFile.isPresent()) {
            File f = currentFile.get();
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
    
    @Override
    protected void __accept(URLConnection t, InputStream inputStream) {
        Optional<File> file = Optional.empty();
        Optional<FileOutputStream> outputStream = Optional.empty();
        try {
            getStreamMetaData().setMetaDataConsumer(m -> {
                this.previousMetaData = metaData;
                this.metaData = m;
                metaDataChanged = true;
                new ConsoleMetaDataConsumer().accept(m);
            });
            byte buffer[] = new byte[BUFFER_SIZE];
            Optional<MimeType> contentType = MimeType.byContentType(t.getContentType());

            if (! getContext().isSongNames()) {
                File f = getNumberFile(contentType);
                file = Optional.of(f);
                outputStream = Optional.of(new FileOutputStream(f));
                log.info("Copying from url {} to file {}, type {}",
                        getContext().getUrl().toExternalForm(),
                        f,
                        contentType);
            }
            
            int len;
            long ofs = 0;
            while (-1 != (len = inputStream.read(buffer))) {
                if (needToAbort(file)) {
                    return;
                }
                
                if (metaDataChanged) {
                    log.debug("Meta data changed");
                    metaDataChanged = false;
                    closeStreamIfOpen(outputStream, file, contentType);
                    
                    file = getFileFromMetaData(contentType);
                    log.debug("New file {}", file);
                    outputStream = file.isPresent() ? 
                            Optional.of(new FileOutputStream(file.get())) : 
                            Optional.empty();
                }
                
                if (outputStream.isPresent()) {
                    outputStream.get().write(buffer, 0, len);
                } else {
                    log.info("Dropped {} bytes, no file name yet", len);                
                }
                ofs += len;
                log.trace("Copied {} bytes", ofs);
            }

        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            fileNumber++;
        } finally {
            if (outputStream.isPresent()) {
                try {
                    outputStream.get().close();
                } catch (IOException ex) {
                    log.warn("URL " + getContext().getUrl().toExternalForm() + " close error", ex);
                }
            }
        }
    }

    private void closeStreamIfOpen(Optional<FileOutputStream> outputStream, Optional<File> file, Optional<MimeType> contentType) throws IOException {
        MetaData fileMetaData = previousMetaData.clone();
        if (outputStream.isPresent()) {
            log.debug("Closing output stream to {}", file.get());
            outputStream.get().close();
            
            if (contentType.isPresent() && contentType.get() == MimeType.AUDIO_MPEG && file.isPresent() && fileMetaData != null) {
                Runnable r = () -> {
                    try {
                        addID3Tags(fileMetaData, file.get());
                    } catch (IOException ex) {
                        log.warn("Error while adding id3 tags", ex);
                    }
                };
                Thread t = new Thread(r, "ID3 "+file.get());
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
            md.getTitle().ifPresent(t -> id3v1.setTitle(t));
            md.getArtist().ifPresent(t -> id3v1.setArtist(t));
            md.getStationName().ifPresent(t -> id3v1.setComment(t));
            mp3File.setId3v1Tag(id3v1);
            
            ID3v24Tag id3v2 = new ID3v24Tag();
            md.getTitle().ifPresent(t -> id3v2.setTitle(t));
            md.getArtist().ifPresent(t -> id3v2.setArtist(t));
            md.getStationName().ifPresent(t -> id3v2.setPublisher(t));
            md.getStationUrl().ifPresent(t -> id3v2.setRadiostationUrl(t));
            id3v2.setComment(Main.PROJECT);
            id3v2.setUrl(Main.GITHUB_URL);
            mp3File.setId3v2Tag(id3v2);
            
            File bak = new File(file.getParentFile(), file.getName()+".bak");
            File tmp = new File(file.getParentFile(), file.getName()+".tmp");
            mp3File.save(tmp.getAbsolutePath());
            
            // foo.mp3 -> foo.mp3.bak
            Files.move(file.toPath(), bak.toPath());
            // foo.mp3.tmp -> foo.mp3
            Files.move(tmp.toPath(), file.toPath());
            // foo.mp3.bak
            Files.delete(bak.toPath());
            
            log.debug("Done adding id3 tag to {}", file);            
        }
        catch (NotSupportedException | UnsupportedTagException | InvalidDataException ex) {
            log.warn("Exception while writing id3 tag for "+file, ex);
        }
    }
    
    

    /** Get the next number based filename.
     * @param contentType content type for calculating the suffix.
     */
    private File getNumberFile(Optional<MimeType> contentType) {
        File f = null;
        do {
            f = new File(getContext().getDirectory(), fileNumber + suffixFromContentType(contentType));
            fileNumber++;
        } while (f.exists() && f.length() != 0);
        return f;
    }
    
    /** Get the file name derived from the received meta data.
     * @return the file, if there is metadata, or empty.
     */
    private Optional<File> getFileFromMetaData(Optional<MimeType> contentType) {
        Optional<File> result = Optional.empty();
        if (metaData != null) {
            File f = null;
            do {                
                String unknown = "unknown";
                String fileName = String.format("%03d.%s - %s%s", fileNumber++, 
                        sanitizeFileName(metaData.getArtist().orElse(unknown)),
                        sanitizeFileName(metaData.getTitle().orElse(unknown)),
                        suffixFromContentType(contentType));
                f = new File(getContext().getDirectory(), fileName);
            } while (f.exists() && f.length() != 0);
            result = Optional.of(f);
        }
        return result;
    }
    
    private final static String sanitizeFileName(String in) {
        return in.replaceAll("[/:\\|]", "_");
    }

    /** Calculate the file suffix. */
    private static String suffixFromContentType(Optional<MimeType> contentType) {
        if (! contentType.isPresent()) {
            return "";
        }
        return contentType.get().getSuffix();
    }
}
