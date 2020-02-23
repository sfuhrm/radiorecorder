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
package de.sfuhrm.radiorecorder.http;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;

/**
 * Configures an URLConnection.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class HttpConnectionBuilderFactory {

    public enum HttpClientType {
        JAVA_NET(JavaNetHttpConnectionBuilder::new),
        APACHE_CLIENT_4(url -> {
            try {
                return new ApacheHttpClient4ConnectionBuilder(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }),
        APACHE_CLIENT_5(url -> {
            try {
                return new ApacheHttpClient5ConnectionBuilder(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        private final Function<URL, HttpConnectionBuilder> builder;

        HttpClientType(@NonNull Function<URL, HttpConnectionBuilder> inBuilder) {
            this.builder = inBuilder;
        }
        HttpConnectionBuilder builder(@NonNull URL url) {
            return builder.apply(url);
        }
    }

    private final HttpClientType httpClientType;

    public HttpConnectionBuilderFactory(@NonNull HttpClientType httpClientType) {
        log.debug("Using client {}", httpClientType);
        this.httpClientType = httpClientType;
    }

    public HttpConnectionBuilder newInstance(URL url) {
        return httpClientType.builder(url);
    }
}
