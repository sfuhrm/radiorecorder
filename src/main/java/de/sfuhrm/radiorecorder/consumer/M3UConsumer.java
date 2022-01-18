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
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.RadioException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for M3U playlist format URLs.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class M3UConsumer extends AbstractConsumer implements Consumer<HttpConnection> {

    public M3UConsumer(ConsumerContext context) {
        super(context);
    }

    @Override
    protected void _accept(HttpConnection t) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(t.getInputStream()))) {
            List<String> lines = bufferedReader.lines().filter(l -> l.startsWith("http")).collect(Collectors.toList());

            log.debug("Found {} lines with URLs", lines.size());
            for (String line : lines) {
                log.debug("Found line {}", line);
                getConnectionHandler().consume(new URL(line));
            }
        }
        catch (IOException ex) {
            log.warn("URL " + getContext().getUrl().toExternalForm() + " broke down", ex);
            throw new RadioException(true, ex);
        }
    }
}
