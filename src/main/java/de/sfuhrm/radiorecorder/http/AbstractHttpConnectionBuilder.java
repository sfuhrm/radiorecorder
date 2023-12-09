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

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores builder properties.
 * @author Stephan Fuhrmann
 */
@Slf4j
class AbstractHttpConnectionBuilder {
    protected Optional<Integer> connectTimeout = Optional.empty();
    protected Optional<Integer> readTimeout = Optional.empty();
    protected final Map<String, String> requestProperties = new HashMap<>();
    protected Optional<URI> proxy = Optional.empty();

    /** Configures the timeout for connecting to the server.
     * @param timeout the timeout in milliseconds.
     */
    public void setConnectTimeout(int timeout) {
        log.debug("Connect timeout: {}", timeout);
        connectTimeout = Optional.of(timeout);
    }

    /** Configures the timeout for reading from the server.
     * @param timeout the timeout in milliseconds.
     */
    public void setReadTimeout(int timeout) {
        log.debug("Read timeout: {}", timeout);
        readTimeout = Optional.of(timeout);
    }

    /** Adds an HTTP request header field to the request.
     * @param key the header field name, for example "User-Agent".
     * @param value the header field value.
     */
    public void setRequestProperty(String key, String value) {
        log.debug("Request property: {} = {}", key, value);
        requestProperties.put(key, value);
    }

    /** Sets the HTTP/HTTPS proxy to use.
     * @param proxy the URL of the proxy to use.
     */
    public void setProxy(URI proxy) {
        log.debug("Proxy: {}", proxy.toASCIIString());
        this.proxy = Optional.of(proxy);
    }
}
