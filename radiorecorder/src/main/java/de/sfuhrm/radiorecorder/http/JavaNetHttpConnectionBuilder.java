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
import java.net.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures an URLConnection.
 * @author Stephan Fuhrmann
 */
@Slf4j
class JavaNetHttpConnectionBuilder extends AbstractHttpConnectionBuilder implements HttpConnectionBuilder {
    private final URI url;

    JavaNetHttpConnectionBuilder(URI url) {
        this.url = url;
    }

    @Override
    public HttpConnection build() throws IOException {

        Proxy proxyToUse = Proxy.NO_PROXY;

        if (proxy.isPresent()) {
            proxyToUse = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxy.get().getHost(), proxy.get().getPort()));
        }
        HttpURLConnection connection = (HttpURLConnection)url.toURL().openConnection(proxyToUse);

        readTimeout.ifPresent(connection::setReadTimeout);

        connectTimeout.ifPresent(connection::setConnectTimeout);

        if (! requestProperties.isEmpty()) {
            requestProperties.forEach(connection::setRequestProperty);
        }

        return new JavaNetHttpConnection(connection);
    }
}
