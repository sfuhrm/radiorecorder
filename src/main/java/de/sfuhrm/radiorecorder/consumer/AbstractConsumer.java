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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.MDC;

/**
 * Base-class for many consumers.
 * Subclasses need to implement {@link #_accept(java.net.URLConnection) }.
 * @author Stephan Fuhrmann
 */
@Slf4j
public abstract class AbstractConsumer implements Consumer<URLConnection> {

    @Getter
    private final ConsumerContext context;
    
    @Getter
    private final ConnectionHandler connectionHandler;

    public AbstractConsumer(ConsumerContext context) {
        this.context = context;
        this.connectionHandler = new ConnectionHandler(context);
    }
        
    @Override
    public final void accept(URLConnection u) {
        MDC.put("url", getContext().getUrl().toExternalForm());
        log.info("Source URL is {}, real URL is {} and directory is {}", getContext().getUrl(), u.getURL().toExternalForm(), getContext().getDirectory());
        
        if (u instanceof HttpURLConnection) {
            try {
                HttpURLConnection huc = (HttpURLConnection) u;
                log.info("HTTP {} {}", huc.getResponseCode(), huc.getResponseMessage());
                if (log.isDebugEnabled()) {
                    log.debug("HTTP Response Header fields");
                    huc.getHeaderFields()
                            .entrySet()
                            .stream()
                            .forEach(e -> {
                                log.debug("  {}: {}", e.getKey(), e.getValue());
                            });
                }
            } catch (IOException ex) {
                log.warn("Error in HTTP communication", ex);
            }
        }
        
        _accept(u);
        MDC.remove("url");
    }
    
    /** The inner accept implementation. Will get called after applying
     * configuration to the URLConnection passed in.
     * @param u the connection to process.
     */
    protected abstract void _accept(URLConnection u);
}
