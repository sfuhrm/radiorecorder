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
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/** Mockito based test for {@link StreamPlayConsumer}.
 * Yes, this test is playing sound!
 * Unknown what happens when no audio is available, for example
 * in a Docker container.
 * @author Stephan Fuhrmann
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class StreamPlayConsumerTest {

    @Mock
    ConsumerContext consumerContext;

    @Mock
    HttpConnection httpConnection;

    StreamPlayConsumer streamPlayConsumer;

    @BeforeEach
    public void init() {
        streamPlayConsumer = new StreamPlayConsumer(consumerContext);
    }

    private boolean isRunningInGithubActions() {
        return System.getenv("GITHUB_JOB") != null;
    }


    @Test
    void __acceptWithOGG() throws IOException {
        URI uri = URI.create("https://getsamplefiles.com/download/ogg/sample-4.ogg");
        innerTest(uri);
    }

    @Test
    void __acceptWithMP3() throws IOException {
        URI uri = URI.create("https://getsamplefiles.com/download/mp3/sample-4.mp3");
        innerTest(uri);
    }

    // new getAudioFileReader() approach can not work with hidden sun classes
    @Disabled
    @Test
    void __acceptWithWAV() throws IOException {
        URI uri = URI.create("https://getsamplefiles.com/download/wav/sample-4.wav");
        innerTest(uri);
    }

    // M4A is not supported
    @Test
    void __acceptWithM4A() throws IOException {
        URI uri = URI.create("https://getsamplefiles.com/download/m4a/sample-4.m4a");
        innerTest(uri);
    }

    // disabled, endless stream
    @Disabled
    @Test
    void __acceptWithAAC() throws IOException {
        URI uri = URI.create("https://playerservices.streamtheworld.com/api/livestream-redirect/XHPSFMAAC.aac");
        innerTest(uri);
    }

    private void innerTest(URI uri) throws IOException {
        if (isRunningInGithubActions()) {
            log.info("Skipping test, running in github actions");
            return;
        }

        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        Mockito.when(consumerContext.getUri()).thenReturn(uri);
        Mockito.when(httpConnection.getContentType()).thenReturn(urlConnection.getContentType());

        InputStream inputStream = urlConnection.getInputStream();
        streamPlayConsumer.__accept(httpConnection, inputStream);
    }


    @AfterEach
    public void validate() {
        Mockito.validateMockitoUsage();
    }
}
