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
import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/** Copies a stream to one or multiple disk files.
 */
@Slf4j
public class StreamCopyConsumer extends MetaDataConsumer implements Consumer<URLConnection> {

    /** The consecutive file number. */
    private int fileNumber;

    /** The last meta data received. Can be {@code null}. */
    private String metaData;
    
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
                this.metaData = m;
                metaDataChanged = true;
                System.err.println(m);
            });
            byte buffer[] = new byte[BUFFER_SIZE];
            String contentType = t.getContentType();

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
                    if (outputStream.isPresent()) {
                        log.debug("Closing output stream to {}", file.get());
                        outputStream.get().close();
                    }
                    
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

    /** Get the next number based filename.
     * @param contentType content type for calculating the suffix.
     */
    private File getNumberFile(String contentType) {
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
    private Optional<File> getFileFromMetaData(String contentType) {
        Optional<File> result = Optional.empty();
        if (metaData != null) {
            File f = null;
            do {                
                String fileName = String.format("%03d.%s%s", fileNumber++, metaData, suffixFromContentType(contentType));
                f = new File(getContext().getDirectory(), fileName);
            } while (f.exists() && f.length() != 0);
            result = Optional.of(f);
        }
        return result;
    }

    /** Calculate the file suffix. */
    private static String suffixFromContentType(String contentType) {
        switch (contentType) {
            case "audio/mpeg":
                return ".mp3";
            case "audio/ogg":
                return ".ogg";
            case "audio/x-wav":
                return ".wav";
            default:
                return "";
        }
    }
}
