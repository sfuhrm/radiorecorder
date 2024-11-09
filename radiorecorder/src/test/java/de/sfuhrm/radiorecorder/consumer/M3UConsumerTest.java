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
import java.net.URI;

import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mockito based test for {@link M3UConsumer}.
 * @author Stephan Fuhrmann
 */
@ExtendWith(MockitoExtension.class)
public class M3UConsumerTest {

    @Mock
    ConsumerContext consumerContext;

    @BeforeEach
    public void init() {
        Mockito.when(consumerContext.getHttpClient()).thenReturn(HttpConnectionBuilderFactory.HttpClientType.JAVA_NET);
    }

    @Test
    void create() {
        M3UConsumer consumer = new M3UConsumer(consumerContext);
        assertEquals(consumerContext, consumer.getContext());
    }

    @Test
    void accept() throws IOException {
        try (TemporaryFile tmp = new TemporaryFile()) {
            tmp.write(TEST_STRING);

            Mockito.when(consumerContext.getUri()).thenReturn(tmp.getFile().toURI());

            HttpConnection connection = Mockito.mock(HttpConnection.class);
            Mockito.when(connection.getURI()).thenReturn(tmp.getFile().toURI());
            //Mockito.when(connection.getContentType()).thenReturn("audio/x-scpls");
            Mockito.when(connection.getInputStream()).thenReturn(tmp.getInputStream());

            ConnectionHandler connectionHandler = Mockito.mock(ConnectionHandler.class);

            M3UConsumer consumer = new M3UConsumer(consumerContext);
            consumer.setConnectionHandler(connectionHandler);
            consumer.accept(connection);

            Mockito.verify(connectionHandler).consume(URI.create("http://streamexample.com:80"));
            Mockito.verify(connectionHandler).consume(URI.create("http://example.com/song.mp3"));
        }
    }

    @AfterEach
    public void validate() {
        Mockito.validateMockitoUsage();
    }

    private final static String TEST_STRING
            = "# foo\n"
            + "http://streamexample.com:80\n"
            + "http://example.com/song.mp3\n"
            + "D:\\Eigene Musik\\album.flac\n";
}
