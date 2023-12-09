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

import de.sfuhrm.radiorecorder.ConnectionHandler;
import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import java.io.IOException;
import java.net.URL;

import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mockito based test for {@link XSPFConsumer}.
 * @author Stephan Fuhrmann
 */
@ExtendWith(MockitoExtension.class)
public class XSPFConsumerTest {

    @Mock
    ConsumerContext consumerContext;

    @BeforeEach
    public void init() {
        Mockito.when(consumerContext.getHttpClient()).thenReturn(HttpConnectionBuilderFactory.HttpClientType.JAVA_NET);
    }

    @Test
    void create() {
        XSPFConsumer consumer = new XSPFConsumer(consumerContext);
        assertEquals(consumerContext, consumer.getContext());
    }

    @Test
    void accept() throws IOException {
        URL url = getClass().getResource("/playlist.xspf");

        assertNotNull(url);

        Mockito.when(consumerContext.getUrl()).thenReturn(url);

        HttpConnection connection = Mockito.mock(HttpConnection.class);
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getInputStream()).thenReturn(url.openStream());

        ConnectionHandler connectionHandler = Mockito.mock(ConnectionHandler.class);

        XSPFConsumer consumer = new XSPFConsumer(consumerContext);
        consumer.setConnectionHandler(connectionHandler);
        consumer.accept(connection);

        Mockito.verify(connectionHandler).consume(new URL("http://stream.futuremusic.fm:8000/mp3"));
        Mockito.verify(connectionHandler).consume(new URL("http://szpila-radio.pl:8300/live.m3u"));
        Mockito.verify(connectionHandler).consume(new URL("http://stream.szpila-radio.pl:8300/listen1"));
        Mockito.verify(connectionHandler).consume(new URL("http://85.25.43.55/berlin.mp3"));
    }

    @AfterEach
    public void validate() {
        Mockito.validateMockitoUsage();
    }
}
