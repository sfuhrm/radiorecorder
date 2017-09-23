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
import java.net.HttpURLConnection;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures an URLConnection.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class JavaNetHttpConnectionBuilder implements HttpConnectionBuilder {
    private final URL url;
    private HttpURLConnection connection;
    
    JavaNetHttpConnectionBuilder(URL url) {
        this.url = url;
    }
    
    private HttpURLConnection connection() throws IOException {
        if (connection == null) {
            connection = (HttpURLConnection)url.openConnection();
        }
        return connection;
    }
    
    @Override
    public void setConnectTimeout(int timeout) throws IOException {
        log.debug("Connect timeout is {}", timeout);
        connection().setConnectTimeout(timeout);
    }

    @Override
    public void setReadTimeout(int timeout) throws IOException {
        log.debug("Read timeout is {}", timeout);
        connection().setReadTimeout(timeout);
    }

    @Override
    public void setRequestProperty(String key, String value) throws IOException {
        log.debug("Request property {} => {}", key, value);
        connection().setRequestProperty(key, value);
    }
    
    @Override
    public HttpConnection build() throws IOException {
        return new JavaNetHttpConnection(connection());
    }

    @Override
    public void close() throws IOException {
        connection.disconnect();
    }
}
