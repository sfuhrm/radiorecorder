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
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.RadioException;
import de.sfuhrm.radiorecorder.metadata.StreamMetaData;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Opens a stream and intercepts possible meta data information.
 * Subclasses need to implement {@link #__accept(de.sfuhrm.radiorecorder.http.HttpConnection, java.io.InputStream) }.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public abstract class MetaDataConsumer extends AbstractConsumer implements Consumer<HttpConnection> {

    @Getter
    private final StreamMetaData streamMetaData;

    public MetaDataConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        streamMetaData = new StreamMetaData();
    }

    @Override
    protected final void _accept(HttpConnection t) {
        try {
            InputStream inputStream = streamMetaData.openStream(t);
            __accept(t, inputStream);
        } catch (IOException ex) {
            log.warn("Failed to open", ex);
            throw new RadioException(true, ex);
        }
    }
    
    protected abstract void __accept(HttpConnection t, InputStream inputStream);
}
