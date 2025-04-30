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
import de.sfuhrm.radiorecorder.Radio;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Mockito based test for {@link MetaDataFileNameGenerator}.
 * @author Stephan Fuhrmann
 */
@ExtendWith(MockitoExtension.class)
public class MetaDataFileNameGeneratorTest {

    @Mock
    ConsumerContext consumerContext;
    Radio radio;
    MetaDataFileNameGenerator instance;
    String filenameFormat = "${radioName}/${artist} - ${title}${suffix}";

    @BeforeEach
    public void init() {
        radio = new Radio();
        radio.setName("Radio Example");
        radio.setUri(URI.create("https://www.radio-browser.info/"));
        radio.setUuid(UUID.randomUUID());
        radio.setTags(Arrays.asList("pop", "rock"));
        radio.setBitrate(123);

        Mockito.when(consumerContext.getTargetDirectory()).thenReturn(Paths.get("/the/music"));

        instance = new MetaDataFileNameGenerator(filenameFormat, consumerContext, true);
    }

    @Test
    void getFileFromWithNothingSet() {
        Optional<Path> actual = instance.getFileFrom(radio, null, null);

        // no content type, no file
        assertEquals(Optional.empty(), actual);
    }

    @Test
    void getFileFromWithAll() {
        MetaData.MetaDataBuilder builder = MetaData.builder();
        builder.artist(Optional.of("Michael Jackson"));
        builder.title(Optional.of("Thriller"));
        builder.stationUrl(Optional.empty());
        builder.index(Optional.empty());


        Optional<Path> actual = instance.getFileFrom(radio, builder.build(), MimeType.AUDIO_MPEG);

        // no content type, no file
        assertEquals(Optional.of(Paths.get("/the/music/Radio Example/Michael Jackson - Thriller.mp3")), actual);
    }

    @Test
    void getFileFromWithEscaping() {
        MetaData.MetaDataBuilder builder = MetaData.builder();
        builder.artist(Optional.of("Are you listening?"));
        builder.title(Optional.of("The Music: /\\"));
        builder.stationUrl(Optional.empty());
        builder.index(Optional.empty());


        Optional<Path> actual = instance.getFileFrom(radio, builder.build(), MimeType.AUDIO_MPEG);

        // replaced funny characters
        assertEquals(Optional.of(Paths.get("/the/music/Radio Example/Are you listening_ - The Music_ __.mp3")), actual);
    }
}
