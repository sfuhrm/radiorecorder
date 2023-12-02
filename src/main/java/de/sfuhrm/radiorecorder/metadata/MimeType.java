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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;

/**
 * Mime types for use in the application.
 * @author Stephan Fuhrmann
 */
public enum MimeType {
    /** MIME type for {@code audio/mpeg}. */
    AUDIO_MPEG("audio/mpeg", ".mp3"),
    /** MIME type for {@code audio/ogg}. */
    AUDIO_OGG("audio/ogg", ".ogg"),
    /** MIME type for {@code audio/x-wav}. */
    AUDIO_XWAV("audio/x-wav", ".wav"),
    /** MIME type for {@code audio/x-ms-wma}. */
    AUDIO_XMSWMA("audio/x-ms-wma", ".wma"),
    /** MIME type for {@code audio/mpegurl}. */
    AUDIO_MPEGURL("audio/mpegurl", ".m3u"),
    /** MIME type for {@code audio/x-mpegurl}. */
    AUDIO_XMPEGURL("audio/x-mpegurl", ".m3u"),
    /** MIME type for {@code audio/x-scpls}. */
    AUDIO_XSCPLS("audio/x-scpls", ".pls"),
    /** MIME type for {@code application/ogg}. */
    APPLICATION_OGG("application/ogg", ".ogg"),
    /** MIME type for {@code application/pls+xml}. */
    APPLICATION_PLS_XML("application/pls+xml", ".pls"),
    /** MIME type for {@code audio/aac}, {@code audio/aacp} or {@code audio/mp4}. */
    AUDIO_AAC(new String[] {"audio/aac", "audio/aacp", "audio/mp4"}, ".m4a");

    /** The content type, for example {@code  audio/aac}. */
    @Getter
    private final String[] contentTypes;

    /** The file system suffix for this mime type, for example {@code .mp3}. */
    @Getter
    private final String suffix;

    /** Constructor for MimeType.
     * @param contentType the content type, for example {@code  audio/aac}.
     * @param suffix file system suffix for this mime type, for example {@code .mp3}.
     * */
    MimeType(@NonNull String contentType, @NonNull String suffix) {
        this.contentTypes = new String[] { contentType };
        this.suffix = suffix;
    }

    /** Constructor for MimeType.
     * @param contentTypes multiple content types to match.
     * @param suffix file system suffix for this mime type, for example {@code .mp3}.
     * */
    MimeType(@NonNull String[] contentTypes, @NonNull String suffix) {
        this.contentTypes = Arrays.copyOf(contentTypes, contentTypes.length);
        this.suffix = suffix;
    }

    boolean matches(String contentTypeToMatch) {
        for (String contentType : contentTypes) {
            if (contentTypeToMatch.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
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
                .filter(mt -> mt.matches(contentType))
                .findFirst();
    }
}
