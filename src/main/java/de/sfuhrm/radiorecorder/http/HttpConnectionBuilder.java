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

/**
 * Configures an HttpConnection. The connection can be created
 * after construction.
 * @see #build() {@code build()} for building a HttpConnection
 * @author Stephan Fuhrmann
 */
public interface HttpConnectionBuilder extends AutoCloseable {
    /** Configures the timeout for connecting to the server.
     * @param timeout the timeout in milliseconds.
     */
    void setConnectTimeout(int timeout) throws IOException;

    /** Configures the timeout for reading from the server.
     * @param timeout the timeout in milliseconds.
     */
    void setReadTimeout(int timeout) throws IOException;

    /** Adds a HTTP request header field to the request.
     * @param key the header field name, for example "User-Agent".
     * @param value the header field value.
     */
    void setRequestProperty(String key, String value) throws IOException;

    /** Constructs a connection from this builder.
     * The connection is usually being opened by this method.
     * @return an open HTTP connection.
     */
    HttpConnection build() throws IOException;

    @Override
    void close();
}
