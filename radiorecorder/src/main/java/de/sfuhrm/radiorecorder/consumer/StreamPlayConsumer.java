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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.sfuhrm.radiorecorder.metadata.MimeType;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jaad.spi.javasound.AACAudioFileReader;

/**
 * Plays a stream using the Java Media Framework API.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamPlayConsumer extends MetaDataConsumer implements Consumer<HttpConnection> {

    /** Constructor.
     * @param consumerContext the context to work in.
     * */
    public StreamPlayConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
    }

    private AudioInputStream getAudioInputStream(String contentType, InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        if (contentType == null) {
            log.error("Content type is null");
            throw new UnsupportedAudioFileException("No mapping for NULL content type");
        }
        Optional<MimeType> mimeType = MimeType.byContentType(contentType);
        if (! mimeType.isPresent()) {
            log.error("Derived mime type is null");
            throw new UnsupportedAudioFileException("No mapping for NULL mime type");
        }
        AudioInputStream result;
        switch (mimeType.get()) {
            case AUDIO_AAC:
                log.debug("Using hard-wired AAC plugin for content-type {}", contentType);
                result = new AACAudioFileReader().getAudioInputStream(inputStream);
                break;
            default:
                // default: do auto recognition
                log.debug("Using auto-detected plugin for content-type {}", contentType);
                result = AudioSystem.getAudioInputStream(inputStream);
                break;
        }
        return result;
    }

    @Override
    protected void __accept(HttpConnection t, InputStream inputStream) {
        try {
            getStreamMetaData().setMetaDataConsumer(new ConsoleMetaDataConsumer());
            byte[] buffer = new byte[BUFFER_SIZE];

            String contentType = t.getContentType();
            log.debug("Content type {}", contentType);

            // this is not needed, but will make the AAC codec fail in an
            // endless loop because it is thinking MP3 can be interpreted as AAC
            //AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(t.getURI().toURL());

            // many audio codecs need mark() and reset() to work
            if (! inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream);
            }

            AudioInputStream input = getAudioInputStream(contentType, inputStream);
            log.debug("Input format {}", input.getFormat());

            boolean bigEndian = input.getFormat().isBigEndian();
            AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, bigEndian);

            log.debug("Target format {}", targetFormat);
            AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, input);
            Mixer.Info mixerInfo = getContext().getMixerInfo();
            try (SourceDataLine line = AudioSystem.getSourceDataLine(targetFormat, mixerInfo)) {
                long bufferSize = line.getBufferSize();
                log.debug("Streaming from url {} to line {}, format {}, buffer size {}",
                        getContext().getUri().toASCIIString(),
                        line.getLineInfo().toString(),
                        contentType,
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
                } finally {
                    line.stop();
                }
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            log.warn("URL " + getContext().getUri().toASCIIString() + " broke down", ex);
            throw new RadioException(false, ex);
        }
    }
}
