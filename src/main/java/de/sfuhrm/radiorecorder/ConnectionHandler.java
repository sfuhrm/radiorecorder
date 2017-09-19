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
package de.sfuhrm.radiorecorder;

import de.sfuhrm.radiorecorder.consumer.M3UConsumer;
import de.sfuhrm.radiorecorder.consumer.PLSConsumer;
import de.sfuhrm.radiorecorder.consumer.StreamCastConsumer;
import de.sfuhrm.radiorecorder.consumer.StreamCopyConsumer;
import de.sfuhrm.radiorecorder.consumer.StreamPlayConsumer;
import de.sfuhrm.radiorecorder.consumer.XSPFConsumer;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles a single URLConnection.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class ConnectionHandler {
    
    private final ConsumerContext consumerContext;

    public ConnectionHandler(ConsumerContext consumerContext) {
        this.consumerContext = Objects.requireNonNull(consumerContext);
    }

    /** Configure the timeout for the conncetion.
     * @param connection the connection to configure.
     */
    protected void configureTimeout(URLConnection connection) {
        connection.setConnectTimeout(consumerContext.getTimeout());
        connection.setReadTimeout(consumerContext.getTimeout());
    }
    
    /** Set headers to motivate Icecast servers to send meta data.
     * @param connection the connection to configure.
     * @see <a href="https://anton.logvinenko.name/en/blog/how-to-get-title-from-audio-stream-with-python.html">ID3 and icecast</a>
     */
    protected void configureIcecast(URLConnection connection) {
        connection.setRequestProperty("Icy-Metadata", "1");
    }
    
    protected void configure(URLConnection connection) {
        configureIcecast(connection);
        configureTimeout(connection);
    }
    
    public URLConnection openConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        configure(connection);
        return connection;
    }
    
    public void consume(URL url) throws IOException {
        boolean first = true;
        do {
            if (!first) {
                log.info("Reconnecting.");
            }
            URLConnection connection = openConnection(url);
            Consumer<URLConnection> consumer = consumerFromContentType(consumerContext, connection.getContentType());
            consumer.accept(connection);
            first = false;
        } while (consumerContext.isReconnect());
    }
    
    public static Consumer<URLConnection> consumerFromContentType(ConsumerContext cc, String contentType) {
        Optional<MimeType> mimeType = MimeType.byContentType(contentType);
        if (!mimeType.isPresent()) {
            log.warn("Unknown content type {}", contentType);
            return t -> {};
        }
        
        switch (mimeType.get()) {
            case AUDIO_MPEG:
            case AUDIO_OGG:
            case APPLICATION_OGG:
            case AUDIO_XWAV:
            case AUDIO_XMSWMA:
                if (cc.getCastReceiver() != null) {
                    return new StreamCastConsumer(cc);
                } else
                if (cc.isPlaying()) {
                    return new StreamPlayConsumer(cc);
                } else {
                    return new StreamCopyConsumer(cc);
                }
            case AUDIO_MPEGURL:
            case AUDIO_XMPEGURL:
                return new M3UConsumer(cc);
//                return new XSPFConsumer(cc);
            case APPLICATION_PLS_XML:
            case AUDIO_XSCPLS:
                return new PLSConsumer(cc);
            default:
                log.warn("Unknown content type {}", contentType);
                return t -> {
                };
        }
    }    
}
