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
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilder;
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles a single {@link HttpConnection} to an internet radio.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class ConnectionHandler {

    private final ConsumerContext consumerContext;
    private final HttpConnectionBuilderFactory builderFactory;

    /** Constructor for {@link ConnectionHandler}.
     * @param consumerContext non-null consumer context this handler belongs to.
          * */
    public ConnectionHandler(@NonNull ConsumerContext consumerContext) {
        this.consumerContext = Objects.requireNonNull(consumerContext);
        this.builderFactory = new HttpConnectionBuilderFactory(consumerContext.getHttpClient());
    }

    /** Configure the timeout for the connection.
     * @param builder the connection to configure.
     */
    protected void configureTimeout(HttpConnectionBuilder builder) {
        builder.setConnectTimeout(consumerContext.getTimeout());
        builder.setReadTimeout(consumerContext.getTimeout());
    }

    /** Configure the proxy for the connection.
     * @param builder the connection to configure.
     */
    protected void configureProxy(HttpConnectionBuilder builder) {
        if (consumerContext.getProxy() != null) {
            builder.setProxy(consumerContext.getProxy());
        }
    }

    /** Set headers to motivate Icecast servers to send meta data.
     * @param builder the connection to configure.
     * @see <a href="https://anton.logvinenko.name/en/blog/how-to-get-title-from-audio-stream-with-python.html">ID3 and icecast</a>
     */
    protected void configureIcecast(HttpConnectionBuilder builder) {
        builder.setRequestProperty("Icy-Metadata", "1");
    }

    /** Set headers for user client.
     * @param builder the connection to configure.
     */
    protected void configureClient(HttpConnectionBuilder builder) {
        builder.setRequestProperty("User-Agent", Main.PROJECT);
    }

    /** Configures the builder with the configuration
     * from the {@link #consumerContext}.
     * @param builder the builder to configure.
     * @throws IOException if configuration fails due to an IO problem.
     * */
    protected void configure(@NonNull HttpConnectionBuilder builder) throws IOException {
        configureIcecast(builder);
        configureTimeout(builder);
        configureClient(builder);
        configureProxy(builder);
    }

    /** Opens the url using a configured connection. */
    private HttpConnection openConnection(URL url) throws RadioException {
        try {
            HttpConnectionBuilder builder = builderFactory.newInstance(url);
            configure(builder);
            return builder.build();
        } catch (IOException ex) {
            throw new RadioException(true, ex);
        }
    }

    private static final long GRACE_PERIOD = 5000;

    /** Consumes the given URL.
     * @param url the URL to process. Must be non-null.
     * @throws NullPointerException if url is null.
     * */
    public void consume(@NonNull URL url) {
        boolean first = true;
        boolean loop = consumerContext.isReconnect();
        do {
            if (!first) {
                log.info("Sleeping for {} millis before retry", GRACE_PERIOD);
                try {
                    Thread.sleep(GRACE_PERIOD);
                } catch (InterruptedException ex) {
                }
                log.info("Reconnecting.");
            }
            try {
                HttpConnection connection = openConnection(url);
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
            case AUDIO_AAC:
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
