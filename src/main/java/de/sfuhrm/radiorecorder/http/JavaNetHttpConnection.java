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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Wrapper for {@link HttpURLConnection}.
 * @author Stephan Fuhrmann
 */
class JavaNetHttpConnection implements HttpConnection {
    private final HttpURLConnection connection;

    JavaNetHttpConnection(HttpURLConnection connection) {
        this.connection = connection;
    }

    @Override
    public URI getURI() {
        try {
            return connection.getURL().toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields()
                .entrySet()
                .stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public String getContentType() {
        return connection.getContentType();
    }

    @Override
    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return connection.getResponseMessage();
    }

    @Override
    public void close() {
        connection.disconnect();
    }
}
