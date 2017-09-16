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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamPlayConsumer extends AbstractConsumer implements Consumer<URLConnection> {

    public StreamPlayConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
    }

    @Override
    protected void _accept(URLConnection t) {
        try {
            byte buffer[] = new byte[BUFFER_SIZE];
            InputStream inputStream = t.getInputStream();

            AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(t.getURL());
            try (SourceDataLine line = AudioSystem.getSourceDataLine(audioFileFormat.getFormat())) {
                log.info("Streaming from url {} to line {}, format {}",
                        getContext().getUrl().toExternalForm(),
                        line.getLineInfo().toString(),
                        audioFileFormat);
                int len;
                long ofs = 0;
                line.open(audioFileFormat.getFormat());
                while (-1 != (len = inputStream.read(buffer))) {
                    ofs += len;
                    line.write(buffer, 0, len);
                    log.trace("Copied {} bytes", ofs);
                }

            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
        }
    }
}
