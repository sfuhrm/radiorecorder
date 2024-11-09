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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit based test for {@link MimeType}.
 * @author Stephan Fuhrmann
 */
public class MimeTypeTest {

    @Test
    void existenceOfAudioMpeg() {
        assertThat(MimeType.AUDIO_MPEG, notNullValue());
    }

    @Test
    void byContentTypeWithAudioMpeg() {
        Optional<MimeType> mimeType = MimeType.byContentType("audio/mpeg");
        assertThat(mimeType, is(Optional.of(MimeType.AUDIO_MPEG)));
    }

    @Test
    void byContentTypeWithUnknown() {
        Optional<MimeType> mimeType = MimeType.byContentType("foo/bar");
        assertThat(mimeType, is(Optional.empty()));
    }
}
