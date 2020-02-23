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

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/** Runnable for one radio station stream given in the command line.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class RadioRunnable implements Runnable {

    public static final int BUFFER_SIZE = 8192;
    private final ConsumerContext consumerContext;
    private final ConnectionHandler configurator;

    public RadioRunnable(ConsumerContext consumerContext) {
        this.consumerContext = Objects.requireNonNull(consumerContext);
        this.configurator = new ConnectionHandler(consumerContext);
    }

    @Override
    public void run() {
        MDC.put("id", Integer.toString(consumerContext.getId()));
        log.info("URL is {} and directory is {}", consumerContext.getUrl(), consumerContext.getDirectory());
        try {
            configurator.consume(consumerContext.getUrl());
        }
        finally {
            MDC.remove("id");
        }
    }
}
