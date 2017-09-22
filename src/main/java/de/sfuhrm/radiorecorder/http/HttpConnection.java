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
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for URLConnection.
 * @author Stephan Fuhrmann
 */
public class HttpConnection {
    private final HttpURLConnection connection;

    HttpConnection(HttpURLConnection connection) {
        this.connection = connection;
    }

    public URL getURL() {
        return connection.getURL();
    }

    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public String getContentType() {
        return connection.getContentType();
    }
    
    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }
    
    public String getResponseMessage() throws IOException {
        return connection.getResponseMessage();
    }
}
