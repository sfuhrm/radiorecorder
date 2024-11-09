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

/** Mockito based test for {@link PLSConsumer}.
 * @author Stephan Fuhrmann
 */
@ExtendWith(MockitoExtension.class)
public class PLSConsumerTest {

    @Mock
    ConsumerContext consumerContext;

    @BeforeEach
    public void init() {
        Mockito.when(consumerContext.getHttpClient()).thenReturn(HttpConnectionBuilderFactory.HttpClientType.JAVA_NET);
    }

    @Test
    void create() {
        PLSConsumer consumer = new PLSConsumer(consumerContext);
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

            PLSConsumer consumer = new PLSConsumer(consumerContext);
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
            = "[playlist]\n"
            + "NumberOfEntries=3\n"
            + "File1=http://streamexample.com:80\n"
            + "Title1=My Favorite Online Radio\n"
            + "Length1=-1\n"
            + "File2=http://example.com/song.mp3\n"
            + "Title2=Remote MP3\n"
            + "Length2=286\n"
            + "File3=D:\\Eigene Musik\\album.flac\n"
            + "Title3=Local album\n"
            + "Length3=3487\n"
            + "Version=2\n";
}
