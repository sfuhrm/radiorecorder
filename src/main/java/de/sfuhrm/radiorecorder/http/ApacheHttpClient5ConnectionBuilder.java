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
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Builder for an Apache HttpClient v5 based connection.
 * @author Stephan Fuhrmann
 */
@Slf4j
class ApacheHttpClient5ConnectionBuilder extends AbstractHttpConnectionBuilder implements HttpConnectionBuilder {

    private final RequestConfig.Builder configBuilder;
    private final ClassicRequestBuilder requestBuilder;

    ApacheHttpClient5ConnectionBuilder(URL url) throws URISyntaxException {
        configBuilder = RequestConfig.custom();
        requestBuilder = ClassicRequestBuilder.get(url.toURI());

        log.debug("Request for uri {}", requestBuilder.getUri());
    }

    @Override
    public HttpConnection build() throws IOException {
        if (connectTimeout.isPresent()) {
            configBuilder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.get()));
            configBuilder.setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout.get()));
        }

        readTimeout.ifPresent(integer -> configBuilder.setResponseTimeout(Timeout.ofMilliseconds(integer)));
        if (! requestProperties.isEmpty()) {
            requestProperties
                    .forEach(requestBuilder::addHeader);
        }
        if (proxy.isPresent()) {
            HttpHost proxyHost = new HttpHost(proxy.get().getProtocol(), proxy.get().getHost(), proxy.get().getPort());
            configBuilder.setProxy(proxyHost);
        }
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(configBuilder.build()).build();
        return new ApacheHttpClient5Connection(client, client.execute(requestBuilder.build()), requestBuilder.getUri());
    }
}
