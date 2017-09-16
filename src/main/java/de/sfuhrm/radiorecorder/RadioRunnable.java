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

import de.sfuhrm.radiorecorder.consumer.GenericConsumer;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RadioRunnable implements Runnable {

    public final static int BUFFER_SIZE = 8192;
    private final ConsumerContext consumerContext;

    public RadioRunnable(ConsumerContext consumerContext) {
        this.consumerContext = Objects.requireNonNull(consumerContext);
    }

    @Override
    public void run() {
        log.info("URL is {} and directory is {}", consumerContext.getUrl(), consumerContext.getDirectory());
        try {
            URLConnection connection = consumerContext.getUrl().openConnection();
            GenericConsumer consumer = new GenericConsumer(consumerContext);
            consumer.accept(connection);
        } 
        catch (IOException ex) {
            log.warn("URL " + consumerContext.getUrl().toExternalForm() + " broke down", ex);
        }
    }
}
