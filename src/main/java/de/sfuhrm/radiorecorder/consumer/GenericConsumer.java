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
public class GenericConsumer extends AbstractConsumer implements Consumer<URLConnection> {

    public GenericConsumer(ConsumerContext context) {
        super(context);
    }

    @Override
    protected void _accept(URLConnection u) {
        log.info("URL is {} and directory is {}", getContext().getUrl(), getContext().getDirectory());
        String contentType = u.getContentType();
        Consumer<URLConnection> consumer = consumerFromContentType(contentType);
        log.debug("Found consumer of type {}", consumer.getClass().getName());
        consumer.accept(u);
    }

    private Consumer<URLConnection> consumerFromContentType(String contentType) {
        switch (contentType) {
            case "audio/mpeg":
            case "audio/ogg":
            case "audio/x-wav":
                if (getContext().isPlaying()) {
                    return new StreamPlayConsumer(getContext());
                } else {
                    return new StreamCopyConsumer(getContext());
                }
            case "audio/mpegurl":
            case "audio/x-mpegurl":
                return new M3UConsumer(getContext());
            case "application/pls+xml":
            case "audio/x-scpls":
                return new PLSconsumer(getContext());
            default:
                log.warn("Unknown content type {}", contentType);
                return t -> {
                };
        }
    }
}
