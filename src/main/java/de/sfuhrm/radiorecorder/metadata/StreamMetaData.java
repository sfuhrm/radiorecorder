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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The meta data collected from a running stream.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamMetaData {
    
    private OffsetFilterStream offsetFilterStream;
    private IcyMetaFilterStream icyMetaFilterStream;
    
    private final static String ICY_METAINT = "icy-metaint";
    
    @Getter @Setter
    private Consumer<MetaData> metaDataConsumer = l -> {};
    
    /** Current meta data. */
    private MetaData metaData = new MetaData();
    
    public InputStream openStream(HttpConnection connection) throws IOException {
        InputStream result;
        offsetFilterStream = new OffsetFilterStream(connection.getInputStream());
        result = offsetFilterStream;
        
        Map<String,List<String>> headers = connection.getHeaderFields();
        if (headers.containsKey("icy-name")) {
            metaData.setStationName(Optional.of(headers.get("icy-name").get(0)));
        }
        if (headers.containsKey("icy-url")) {
            metaData.setStationUrl(Optional.of(headers.get("icy-url").get(0)));
        }
        
        if (headers.containsKey(ICY_METAINT)) {
            log.debug("Found Icy Meta Interval header: {}", headers.containsKey(ICY_METAINT));
            int metaInterval = Integer.parseInt(headers.get(ICY_METAINT).get(0));
            icyMetaFilterStream = new IcyMetaFilterStream(metaInterval, offsetFilterStream);
            icyMetaFilterStream.setMetaDataConsumer(md -> {
                Pattern p = Pattern.compile("(.{2,}) - (.{2,})");
                Matcher m = p.matcher(md);
                MetaData target = metaData.clone();
                target.setCreated(ZonedDateTime.now());
                target.setPosition(Optional.of(offsetFilterStream.getOffset()));
                if (m.matches()) {
                    target.setArtist(Optional.of(m.group(1)));
                    target.setTitle(Optional.of(m.group(2)));
                } else {
                    target.setArtist(Optional.empty());
                    target.setTitle(Optional.empty());                    
                }
                metaData = target;
                metaDataConsumer.accept(target);
            });
            result = icyMetaFilterStream;
        }
        
        return result;
    }
}
