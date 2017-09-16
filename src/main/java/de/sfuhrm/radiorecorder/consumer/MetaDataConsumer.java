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
import de.sfuhrm.radiorecorder.metadata.StreamMetaData;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MetaDataConsumer extends AbstractConsumer implements Consumer<URLConnection> {

    @Getter
    private final StreamMetaData streamMetaData;
    
    public MetaDataConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        streamMetaData = new StreamMetaData();
    }

    @Override
    protected final void _accept(URLConnection t) {
        try {
            InputStream inputStream = streamMetaData.openStream(t);
            __accept(t, inputStream);
        } catch (IOException ex) {
            log.warn("Failed to open", ex);
        }
    }
    
    protected abstract void __accept(URLConnection t, InputStream inputStream);
    
}
