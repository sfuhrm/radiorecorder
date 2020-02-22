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
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Mime types for use in the application.
 * @author Stephan Fuhrmann
 */
public enum MimeType {
    AUDIO_MPEG("audio/mpeg", ".mp3"),
    AUDIO_OGG("audio/ogg", ".ogg"),
    AUDIO_XWAV("audio/x-wav", ".wav"),
    AUDIO_XMSWMA("audio/x-ms-wma", ".wma"),
    AUDIO_MPEGURL("audio/mpegurl", ".m3u"),
    AUDIO_XMPEGURL("audio/x-mpegurl", ".m3u"),
    AUDIO_XSCPLS("audio/x-scpls", ".pls"),
    APPLICATION_OGG("application/ogg", ".ogg"),
    APPLICATION_PLS_XML("application/pls+xml", ".pls");

    @Getter
    private final String contentType;

    @Getter
    private final String suffix;

    MimeType(String contentType, String suffix) {
        this.contentType = contentType;
        this.suffix = suffix;
    }

    /** Finds the mime type by content type.
     * @param contentType a content type String for example {@code "audio/mpeg"}.
     * @return the identified enum instance wrapped in an {@code Optional}
     * with the possibility of an empty Optional if not found.
     */
    public static Optional<MimeType> byContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        return Stream.of(MimeType.values())
                .filter(mt -> mt.contentType.equalsIgnoreCase(contentType))
                .findFirst();
    }
}
