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

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;


/**
 * Wrapper for an Apache HttpClient5 based connection.
 * @author Stephan Fuhrmann
 */
class ApacheHttpClient5Connection implements HttpConnection {

    private final CloseableHttpClient client;
    private final CloseableHttpResponse response;
    private final URI uri;

    ApacheHttpClient5Connection(CloseableHttpClient client, CloseableHttpResponse connection, URI uri) {
        this.client = client;
        this.response = connection;
        this.uri = uri;
    }

    @Override
    public URI getURI() throws IOException {
        return uri;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {

        return Stream
                .of(response.getHeaders())
                .filter(h -> h.getValue() != null)
                .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    @Override
    public String getContentType() {
        return response.getFirstHeader("Content-Type").getValue();
    }

    @Override
    public int getResponseCode() {
        return response.getCode();
    }

    @Override
    public String getResponseMessage() {
        return response.getReasonPhrase();
    }

    @Override
    public void close() throws IOException {
        response.close();
        client.close();
    }
}
