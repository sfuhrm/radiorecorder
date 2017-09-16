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
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamCopyConsumer extends AbstractConsumer implements Consumer<URLConnection> {

    private int fileNumber;

    public StreamCopyConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        fileNumber = 1;
    }

    @Override
    protected void _accept(URLConnection t) {
        FileOutputStream outputStream = null;
        try {
            byte buffer[] = new byte[BUFFER_SIZE];

            InputStream inputStream = t.getInputStream();
            String contentType = t.getContentType();

            File f = null;
            do {
                f = new File(getContext().getDirectory(), fileNumber + suffixFromContentType(contentType));
                fileNumber++;
            } while (f.exists() && f.length() != 0);
            outputStream = new FileOutputStream(f);
            
            log.info("Copying from url {} to file {}, type {}", 
                    getContext().getUrl().toExternalForm(), 
                    f,
                    contentType);
            int len;
            long ofs = 0;
            while (-1 != (len = inputStream.read(buffer))) {
                
                FileStore fileStore = Files.getFileStore(f.toPath());
                long free = fileStore.getUsableSpace();
                long required = getContext().getMinFree();
                if (free < required) {
                    log.warn("Path {} is too full, has less than {} bytes free", f, required);
                    return;
                }
                
                outputStream.write(buffer, 0, len);
                ofs += len;
                log.trace("Copied {} bytes", ofs);
            }

        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            fileNumber++;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    log.warn("URL " + getContext().getUrl().toExternalForm() + " close error", ex);
                }
            }
        }
    }

    public static String suffixFromContentType(String contentType) {
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
