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
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for an open HTTP protocol connection.
 * @author Stephan Fuhrmann
 */
public interface HttpConnection extends AutoCloseable {

    /** Get the URL being associated to this connection.
     * @return the URL associated with this connection.
     * @throws IOException if resolving the URL resulted in an error.
     */
    URI getURI() throws IOException;

    /** Get the response header fields from the HTTP server.
     * @return a map with the keys being headers field names and the
     * values being header field values. If a header field is given multiple
     * times the value list will contain all values.
     */
    Map<String, List<String>> getHeaderFields();

    /** Get the input stream reading the HTTP response body.
     * @return the input stream of the connection.
     * @throws IOException if there's a problem getting or opening the
     * input stream.
     */
    InputStream getInputStream() throws IOException;

    /** Get the content type of the stream.
     * @return the content type, for example "audio/mpeg", or
     * {@code null} if not sent by the server.
     */
    String getContentType();

    /** Get the HTTP server response code.
     * @return the numerical response code, for example 200 for "OK".
     * @throws IOException if there is a problem getting the response code.
     * @see #getResponseMessage()
     */
    int getResponseCode() throws IOException;

    /** Get the HTTP server response status message.
     * @return the textual response message, for example "OK".
     * @throws IOException if there is a problem getting the response message.
     * @see #getResponseCode()
     */
    String getResponseMessage() throws IOException;

    @Override
    void close() throws IOException;
}
