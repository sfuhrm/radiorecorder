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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
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
    
    private MetaData metaData = new MetaData();
    
    /** Get the last retrieved meta data or {@code null} if not existing. */
    public String getStreamInfo() {
        if (icyMetaFilterStream != null) {
            return icyMetaFilterStream.getLastMetaData();
        }
        return null;
    }
    
    public InputStream openStream(URLConnection connection) throws IOException {
        InputStream result;
        offsetFilterStream = new OffsetFilterStream(connection.getInputStream());
        result = offsetFilterStream;
        
        if (connection.getHeaderField("icy-name") != null) {
            metaData.setStationName(Optional.of(connection.getHeaderField("icy-name")));
        }
        if (connection.getHeaderField("icy-url") != null) {
            metaData.setStationUrl(Optional.of(connection.getHeaderField("icy-url")));
        }
        
        if (connection.getHeaderField(ICY_METAINT) != null) {
            log.debug("Found Icy Meta Interval header: {}", connection.getHeaderField(ICY_METAINT));
            int metaInterval = connection.getHeaderFieldInt(ICY_METAINT, 0);
            icyMetaFilterStream = new IcyMetaFilterStream(metaInterval, offsetFilterStream);
            icyMetaFilterStream.setMetaDataConsumer(md -> {
                Pattern p = Pattern.compile("(.{2,}) - (.{2,})");
                Matcher m = p.matcher(md);
                MetaData target = metaData.clone();
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
