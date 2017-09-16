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
package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.ConsumerContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for PLS playlist format URLs.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class PLSConsumer extends AbstractConsumer implements Consumer<URLConnection> {

    public PLSConsumer(ConsumerContext context) {
        super(context);
    }
    
    static List<String> readUrls(InputStream i) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(i));
            List<String> urls = bufferedReader
                    .lines()
                    .filter(l -> l.startsWith("File"))
                    .filter(l -> l.contains("="))
                    .map(l -> l.substring(l.indexOf("=") + 1))
                    .filter(l -> l.startsWith("http"))
                    .collect(Collectors.toList());
            return urls;
    }

    @Override
    protected void _accept(URLConnection t) {
        try {
            List<String> urls = readUrls(t.getInputStream());
            for (String url : urls) {
                getConfigurator().consume(new URL(url));
            }
        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
        }
    }
}
