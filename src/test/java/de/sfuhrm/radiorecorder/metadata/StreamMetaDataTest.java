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
package de.sfuhrm.radiorecorder.metadata;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link  StreamMetaData}
 * @author Stephan Fuhrmann
 */
public class StreamMetaDataTest {
    @Test
    public void parseWithEmptyTemplate() {
        MetaData actual = StreamMetaData.parse(new MetaData(), 0, "foo - bar");

        assertEquals(Optional.of("foo"), actual.getArtist());
        assertEquals(Optional.of("bar"), actual.getTitle());
        assertEquals(Optional.of(0), actual.getIndex());
        assertEquals(Optional.of(0L), actual.getOffset());
        assertNotNull(actual.getCreated());
    }

    @Test
    public void parseWithExistingTemplate() {
        MetaData old = new MetaData();
        old.setIndex(Optional.of(1));
        old.setOffset(Optional.of(42L));
        old.setStationUrl(Optional.of("http://www.google.com/"));
        old.setStationName(Optional.of("Google Radio"));
        old.setTitle(Optional.of("Old McDonald had a farm"));
        old.setArtist(Optional.of("Farmer McDonald"));

        MetaData actual = StreamMetaData.parse(old, 43, "dipsy - tinkywinky");

        assertEquals(Optional.of("dipsy"), actual.getArtist());
        assertEquals(Optional.of("tinkywinky"), actual.getTitle());
        assertEquals(Optional.of(2), actual.getIndex());
        assertEquals(Optional.of(43L), actual.getOffset());
    }

    @Test
    public void parseWithMalformedMetadata() {
        MetaData old = new MetaData();
        old.setIndex(Optional.of(1));
        old.setOffset(Optional.of(42L));
        old.setStationUrl(Optional.of("http://www.google.com/"));
        old.setStationName(Optional.of("Google Radio"));
        old.setTitle(Optional.of("Old McDonald had a farm"));
        old.setArtist(Optional.of("Farmer McDonald"));

        MetaData actual = StreamMetaData.parse(old, 66, "this is really malformed");

        assertEquals(Optional.empty(), actual.getArtist());
        assertEquals(Optional.of("this is really malformed"), actual.getTitle());
        assertEquals(Optional.of(2), actual.getIndex());
        assertEquals(Optional.of(66L), actual.getOffset());
    }
}
