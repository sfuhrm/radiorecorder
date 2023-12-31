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

import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The tuple of media metadata.
 * @author Stephan Fuhrmann
 */
public class MetaData implements Cloneable {
    /** The index of the song in the stream. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<Integer> index;

    /** The timestamp when the song was first detected. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private ZonedDateTime created;

    /** The artist of the song. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<String> artist;

    /** The title of the song. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<String> title;

    /** The name of the radio station. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<String> stationName;

    /** The URL of the radio station. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<String> stationUrl;

    /** The byte offset in the stream. */
    @Getter @Setter(AccessLevel.PACKAGE)
    private Optional<Long> offset;

    /** Constructs a metadata objects with created timestamp now and all other
     * fields {@link Optional#empty()}.
     * */
    public MetaData() {
        created = ZonedDateTime.now();
        artist = Optional.empty();
        title = Optional.empty();
        stationName = Optional.empty();
        stationUrl = Optional.empty();
        offset = Optional.empty();
        index = Optional.empty();
    }

    @Override
    public MetaData clone() {
        try {
            return (MetaData) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex); // can not happen
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        artist.ifPresent(builder::append);
        title.ifPresent(t -> builder.append(" - ").append(t));
        stationName.ifPresent(t -> builder.append(" - ").append(t));
        return builder.toString();
    }
}
