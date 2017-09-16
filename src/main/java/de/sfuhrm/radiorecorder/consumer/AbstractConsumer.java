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

@Slf4j
public abstract class AbstractConsumer implements Consumer<URLConnection> {

    @Getter
    private final ConsumerContext context;

    public AbstractConsumer(ConsumerContext context) {
        this.context = context;
    }
    
    protected void configureTimeout(URLConnection connection) {
        connection.setConnectTimeout(context.getTimeout());
        connection.setReadTimeout(context.getTimeout());
    }
    
    @Override
    public void accept(URLConnection u) {
        MDC.put("url", getContext().getUrl().toExternalForm());
        configureTimeout(u);
        log.info("URL is {} and directory is {}", getContext().getUrl(), getContext().getDirectory());
        _accept(u);
        MDC.remove("url");
    }
    
    protected abstract void _accept(URLConnection u);
}
