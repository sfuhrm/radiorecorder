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

import de.sfuhrm.radiorecorder.http.HttpConnection;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The metadata collected from a running stream.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamMetaData {

    private OffsetFilterStream offsetFilterStream;
    private IcyMetaFilterStream icyMetaFilterStream;

    private static final String ICY_METAINT = "icy-metaint";
    private static final String ICY_NAME = "icy-name";
    private static final String ICY_URL = "icy-url";

    @Getter @Setter
    private Consumer<MetaData> metaDataConsumer = l -> {};

    /** Current meta data. */
    private MetaData metaData = new MetaData();

    private static final Pattern artistTitlePattern = Pattern.compile("(.{2,}) - (.{2,})");

    /** Opens the input stream of the http connection.
     * Internally filters the stream and pushes all
     * new metadata objects seen to the registered
     * metadata consumer.
     * @param connection the non-null http connection to open the stream for.
     * @return the inputstream that reads data from the source the HttpConnection provides.
     * @see #metaDataConsumer
     * @throws IOException if a problem occurs while opening the stream.
     * */
    public InputStream openStream(@NonNull HttpConnection connection) throws IOException {
        InputStream result;
        offsetFilterStream = new OffsetFilterStream(connection.getInputStream());
        result = offsetFilterStream;

        // headers in original form
        Map<String,List<String>> headersOriginal = connection.getHeaderFields();
        // headers with keys mapped to lower case
        Map<String,List<String>> headers = headersOriginal
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
        if (headers.containsKey(ICY_NAME)) {
            String stationName = headers.get(ICY_NAME).get(0);
            log.debug("Station name: {}", stationName);
            metaData.setStationName(Optional.of(stationName));
        }
        if (headers.containsKey(ICY_URL)) {
            String stationUrl = headers.get(ICY_URL).get(0);
            log.debug("Station url: {}", stationUrl);
            metaData.setStationUrl(Optional.of(stationUrl));
        }

        if (headers.containsKey(ICY_METAINT)) {
            log.debug("Found Icy Meta Interval header: {}", headers.containsKey(ICY_METAINT));
            int metaInterval = Integer.parseInt(headers.get(ICY_METAINT).get(0));
            icyMetaFilterStream = new IcyMetaFilterStream(metaInterval, offsetFilterStream);
            icyMetaFilterStream.setMetaDataConsumer(md -> {
                MetaData target = parse(metaData, offsetFilterStream.getOffset(), md);
                metaData = target;
                metaDataConsumer.accept(target);
            });
            result = icyMetaFilterStream;
        }

        return result;
    }

    /** Parses the metadata string.
     * @param oldMetaData previous songs meta data (as a template).
     * @param offset byte offset in the stream of this song.
     * @param streamMetaData usually a string containing of arist and title separated by a hyphen '-'.
     * @return meta data object.
     * */
    static MetaData parse(MetaData oldMetaData, long offset, String streamMetaData) {
        Matcher m = artistTitlePattern.matcher(streamMetaData);
        MetaData target = oldMetaData.clone();
        target.setCreated(ZonedDateTime.now());
        // start counting at 0
        target.setIndex(Optional.of(1 + oldMetaData.getIndex().orElse(-1)));
        target.setOffset(Optional.of(offset));
        if (m.matches()) {
            String artist = m.group(1);
            String title = m.group(2);
            log.debug("Icy Meta artist: {}, icy meta title: {}",
                    artist,
                    title);
            target.setArtist(Optional.of(artist));
            target.setTitle(Optional.of(title));
        } else {
            log.info("Icy Meta data was malformed: {}", streamMetaData);
            target.setArtist(Optional.empty());
            target.setTitle(Optional.of(streamMetaData));
        }
        return target;
    }
}
