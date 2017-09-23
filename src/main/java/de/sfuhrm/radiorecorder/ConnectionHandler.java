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
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilder;
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles a single {@link HttpConnection}.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class ConnectionHandler {
    
    private final ConsumerContext consumerContext;
    private final HttpConnectionBuilderFactory builderFactory;

    public ConnectionHandler(ConsumerContext consumerContext) {
        this.consumerContext = Objects.requireNonNull(consumerContext);
        this.builderFactory = new HttpConnectionBuilderFactory();
    }

    /** Configure the timeout for the conncetion.
     * @param builder the connection to configure.
     */
    protected void configureTimeout(HttpConnectionBuilder builder) throws IOException {
        builder.setConnectTimeout(consumerContext.getTimeout());
        builder.setReadTimeout(consumerContext.getTimeout());
    }
    
    /** Set headers to motivate Icecast servers to send meta data.
     * @param builder the connection to configure.
     * @see <a href="https://anton.logvinenko.name/en/blog/how-to-get-title-from-audio-stream-with-python.html">ID3 and icecast</a>
     */
    protected void configureIcecast(HttpConnectionBuilder builder) throws IOException {
        builder.setRequestProperty("Icy-Metadata", "1");
    }
    
    /** Set headers for user client.
     * @param builder the connection to configure.
     */
    protected void configureClient(HttpConnectionBuilder builder) throws IOException {
        builder.setRequestProperty("User-Agent", Main.PROJECT);
    }
    
    protected void configure(HttpConnectionBuilder builder) throws IOException {
        configureIcecast(builder);
        configureTimeout(builder);
        configureClient(builder);
    }
    
    /** Opens the url using a configured connection. */
    private HttpConnection openConnection(URL url) throws IOException {
        try (HttpConnectionBuilder builder = builderFactory.newInstance(url)) {
            configure(builder);
            return builder.build();
        }
    }
    
    /** Consumes the given URL. */
    public void consume(URL url) throws IOException {
        boolean first = true;
        Objects.requireNonNull(url, "url must be non-null");
        boolean loop = consumerContext.isReconnect();
        do {
            if (!first) {
                log.info("Reconnecting.");
            }
            try (HttpConnection connection = openConnection(url)) {
                Consumer<HttpConnection> consumer = consumerFromContentType(consumerContext, connection.getContentType());
                consumer.accept(connection);
                first = false;
                loop = false;
            } catch (RadioException re) {
                loop &= re.isRetryable();
                log.debug("Retrying after {}? retryable={}, will retry={}", re.getMessage(), re.isRetryable(), loop);
            }
        } while (loop);
    }
    
    private static Consumer<HttpConnection> consumerFromContentType(ConsumerContext cc, String contentType) {
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
