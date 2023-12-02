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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;

/**
 * Configures an URLConnection.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class HttpConnectionBuilderFactory {

    /** Multiple types of http clients this program offers. */
    public enum HttpClientType {
        /** Built-in JDK java.net HTTP connection.  */
        JAVA_NET(JavaNetHttpConnectionBuilder::new),
        
        /** Apache Httpcomponents HttpClient 5.x. */
        APACHE_CLIENT_5(url -> {
            try {
                return new ApacheHttpClient5ConnectionBuilder(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        private final Function<URI, HttpConnectionBuilder> builder;

        HttpClientType(@NonNull Function<URI, HttpConnectionBuilder> inBuilder) {
            this.builder = inBuilder;
        }
        HttpConnectionBuilder builder(@NonNull URI url) {
            return builder.apply(url);
        }
    }

    private final HttpClientType httpClientType;

    /** Constructor.
     * @param httpClientType the type of client to produce.
     * */
    public HttpConnectionBuilderFactory(@NonNull HttpClientType httpClientType) {
        log.debug("Using client {}", httpClientType);
        this.httpClientType = httpClientType;
    }

    /** Creates a new client of the type configured in the type.
     * @param url the URL to create a new builder for.
     * @return a new builder instance for the given URL.
     * @see #httpClientType
     * */
    public HttpConnectionBuilder newInstance(URI url) {
        return httpClientType.builder(url);
    }
}
