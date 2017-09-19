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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;

/**
 * Plays a stream using the Java Media Framework API.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamPlayConsumer extends MetaDataConsumer implements Consumer<URLConnection> {

    public StreamPlayConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
    }

    @Override
    protected void __accept(URLConnection t, InputStream inputStream) {
        try {
            getStreamMetaData().setMetaDataConsumer(new ConsoleMetaDataConsumer());
            byte buffer[] = new byte[BUFFER_SIZE];

            AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(t.getURL());
            AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, true);
            AudioInputStream input = AudioSystem.getAudioInputStream(inputStream);
            AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, input);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(targetFormat)) {
                long bufferSize = line.getBufferSize();
                log.info("Streaming from url {} to line {}, format {}, buffer size {}",
                        getContext().getUrl().toExternalForm(),
                        line.getLineInfo().toString(),
                        audioFileFormat,
                        bufferSize);
                int len;
                long ofs = 0;
                line.open(targetFormat);
                try {
                    while (-1 != (len = converted.read(buffer))) {
                        log.trace("Read {} bytes", len);
                        ofs += len;

                        // start the line before blocking
                        if (!line.isRunning() && line.available() < len) {
                            log.debug("Starting line, not yet running, {} / {} available", line.available(), bufferSize);
                            line.start();
                        }

                        line.write(buffer, 0, len);
                        log.trace("Wrote {} bytes (total {})", len, ofs);
                    }
                } catch (IOException ioe) {
                    throw new RadioException(true, ioe);
                }
                line.stop();
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            throw new RadioException(false, ex);
        }
    }
}
