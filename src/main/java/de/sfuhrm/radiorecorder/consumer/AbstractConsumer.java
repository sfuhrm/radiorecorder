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

    public AbstractConsumer(ConsumerContext context) {
        this.context = context;
    }
    
    /** Configure the timeout for the conncetion. */
    protected void configureTimeout(URLConnection connection) {
        connection.setConnectTimeout(context.getTimeout());
        connection.setReadTimeout(context.getTimeout());
    }
    
    @Override
    public void accept(URLConnection u) {
        MDC.put("url", getContext().getUrl().toExternalForm());
        configureTimeout(u);
        log.info("Source URL is {}, real URL is {} and directory is {}", getContext().getUrl(), u.getURL().toExternalForm(), getContext().getDirectory());
        _accept(u);
        MDC.remove("url");
    }
    
    /** The inner accept implementation. Will get called after applying
     * configuration to the URLConnection passed in.
     */
    protected abstract void _accept(URLConnection u);
}
