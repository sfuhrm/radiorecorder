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
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Builder for an Apache HttpClient v5 based connection.
 * @author Stephan Fuhrmann
 */
@Slf4j
class ApacheHttpClient5ConnectionBuilder extends AbstractHttpConnectionBuilder implements HttpConnectionBuilder {

    private final RequestConfig.Builder configBuilder;
    private final ClassicRequestBuilder requestBuilder;

    ApacheHttpClient5ConnectionBuilder(URI url) throws URISyntaxException {
        this.configBuilder = RequestConfig.custom();
        this.requestBuilder = ClassicRequestBuilder.get(url);

        log.debug("Request for uri {}", requestBuilder.getUri());
    }

    @Override
    public HttpConnection build() throws IOException {
        readTimeout.ifPresent(integer -> configBuilder.setResponseTimeout(Timeout.ofMilliseconds(integer)));
        if (! requestProperties.isEmpty()) {
            requestProperties
                    .forEach(requestBuilder::addHeader);
        }

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder = httpClientBuilder.setDefaultRequestConfig(configBuilder.build());

        if (proxy.isPresent()) {
            HttpHost proxyHost = new HttpHost(proxy.get().getScheme(), proxy.get().getHost(), proxy.get().getPort());
            HttpRoutePlanner httpRoutePlanner = new DefaultProxyRoutePlanner(proxyHost);
            httpClientBuilder.setRoutePlanner(httpRoutePlanner);
        }

        if (connectTimeout.isPresent()) {
            ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom();
            connectionConfigBuilder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.get()));
            BasicHttpClientConnectionManager basicHttpClientConnectionManager = new BasicHttpClientConnectionManager();
            basicHttpClientConnectionManager.setConnectionConfig(connectionConfigBuilder.build());
            httpClientBuilder.setConnectionManager(basicHttpClientConnectionManager);
        }

        CloseableHttpClient client = httpClientBuilder.build();

        HttpHost httpHost = HttpHost.create(requestBuilder.getUri());
        ClassicHttpRequest classicHttpRequest = requestBuilder.build();

        return new ApacheHttpClient5Connection(client, client.executeOpen(httpHost, classicHttpRequest, null), requestBuilder.getUri());
    }
}
