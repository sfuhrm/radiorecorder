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
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PLSconsumer extends AbstractConsumer implements Consumer<URLConnection> {

    public PLSconsumer(ConsumerContext context) {
        super(context);
    }
    

    @Override
    protected void _accept(URLConnection t) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(t.getInputStream()));
            List<String> lines = bufferedReader.lines().filter(l -> l.startsWith("File")).collect(Collectors.toList());
            
            for (String line : lines) {
                int sep = line.indexOf("=");
                if (sep == -1) {
                    log.debug("Line doesn't contain '=': {}", line);
                    continue;
                }
                String value = line.substring(sep + 1).trim();
                if (!value.startsWith("http")) {
                    log.debug("Line is not http: {}", line);
                    continue;
                }
                
                new GenericConsumer(getContext()).accept(new URL(value).openConnection());
            }
        } catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
        }
    }
}
