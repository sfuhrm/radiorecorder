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

import de.sfuhrm.radiorecorder.ConnectionHandler;
import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.RadioException;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base-class for many consumers.
 * Subclasses need to implement {@link #_accept(de.sfuhrm.radiorecorder.http.HttpConnection) }.
 * @author Stephan Fuhrmann
 */
@Slf4j
public abstract class AbstractConsumer implements Consumer<HttpConnection> {

    @Getter
    private final ConsumerContext context;

    @Getter @Setter(AccessLevel.PACKAGE)
    private ConnectionHandler connectionHandler;

    /** Constructor for AbstractConsumer.
     * @param context the context to provide to the subclasses via the accessor.
     *                Must be non-null.
     * @throws NullPointerException if context is null.
     * */
    public AbstractConsumer(@NonNull ConsumerContext context) {
        this.context = context;
        this.connectionHandler = new ConnectionHandler(context);
    }

    @Override
    public final void accept(HttpConnection u) {
        try {
            log.debug("Source URL is {}, real URL is {}", getContext().getUrl(), u.getURL().toExternalForm());
            log.debug("HTTP {} {}", u.getResponseCode(), u.getResponseMessage());
            if (log.isDebugEnabled()) {
                log.debug("HTTP Response Header fields");
                u.getHeaderFields()
                        .entrySet()
                        .stream()
                        .forEach(e -> log.debug("  {}: {}", e.getKey(), e.getValue()));
            }
        } catch (IOException ex) {
            log.warn("Error in HTTP communication", ex);
            throw new RadioException(true, ex);
        }

        _accept(u);
    }

    /** The inner accept implementation. Will get called after applying
     * configuration to the HttpConnection passed in.
     * @param u the connection to process.
     */
    protected abstract void _accept(HttpConnection u);
}
