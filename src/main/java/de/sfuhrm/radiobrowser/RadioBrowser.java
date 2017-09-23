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
package de.sfuhrm.radiobrowser;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioBrowser {

    public final static String API_URL = "http://www.radio-browser.info/webservice/";

    private final WebTarget webTarget;
    private final String userAgent;
    private static final Logger logger = LoggerFactory.getLogger(RadioBrowser.class);

    public RadioBrowser(int timeout, String userAgent) {
        Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
        this.userAgent = userAgent;
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT,    timeout);
        webTarget = client.target(API_URL);
    }

    public List<Station> readAllStations(Integer totalLimit) {
        int step = 100;
        List<Station> result = new LinkedList<>();
        for (int i = 0; i < Integer.MAX_VALUE && (totalLimit == null || i < totalLimit); i += step) {
            MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
            requestParams.put("limit", Collections.singletonList(Integer.toString(step)));
            requestParams.put("offset", Collections.singletonList(Integer.toString(i)));
            logger.info("limit={}, offset={}", step, i);
            Entity entity = Entity.form(requestParams);
            Response response = webTarget.path("json/stations")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .header("User-Agent", userAgent)
                    .post(entity);
            logger.debug("response status={}, length={}", response.getStatus(), response.getLength());
            List<Station> list;
            
            list = response.readEntity(new GenericType<List<Station>>() {
            });
            if (list.isEmpty()) {
                break;
            }

            result.addAll(list);
        }
        return result;
    }

    public UrlResponse resolveStreamUrl(Station bStation) {
        // http://www.radio-browser.info/webservice/v2/json/url/stationid 
        Response response = webTarget.path("v2/json/url").path(bStation.id)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("User-Agent", userAgent)
                .get();

        logger.debug("URI is {}", webTarget.getUri());
        if (response.getStatus() != 200) {
            logger.warn("Non 200 status: {} {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            throw new RadioException(response.getStatusInfo().getReasonPhrase());
        }
        UrlResponse response2 = response.readEntity(UrlResponse.class);
        return response2;
    }
    
    public void postNewStation(Station postMe) {
        // http://www.radio-browser.info/webservice/json/add
        MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();
        requestParams.put("name", Collections.singletonList(postMe.name));
        requestParams.put("homepage", Collections.singletonList(postMe.homepage));
        requestParams.put("url", Collections.singletonList(postMe.url));        
        
        Entity entity = Entity.form(requestParams);

        Response response = webTarget.path("json/add")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .accept(MediaType.APPLICATION_JSON_TYPE)
        .header("User-Agent", userAgent)
        .post(entity);

        Map<String, Object> map = response.readEntity(new GenericType<Map<String, Object>>() {});
        logger.debug("Result: {}", map);
    }
}
