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
import java.util.function.Function;

/**
 * Creates a HttpConnectionBuilder.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class HttpConnectionBuilderFactory {

    /** Constructor.
     * */
    public HttpConnectionBuilderFactory() {
    }

    /** Creates a new client of the type configured in the type.
     * @param url the URL to create a new builder for.
     * @return a new builder instance for the given URL.
     * */
    public HttpConnectionBuilder newInstance(URI url) {
        return new ApacheHttpClient5ConnectionBuilder(url);
    }
}
