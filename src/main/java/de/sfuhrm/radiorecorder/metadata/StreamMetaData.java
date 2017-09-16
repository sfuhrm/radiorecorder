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
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author fury
 */
@Slf4j
public class StreamMetaData {
    
    private OffsetFilterStream offsetFilterStream;
    private IcyMetaFilterStream icyMetaFilterStream;
    
    private final static String ICY_METAINT = "icy-metaint";
    
    @Getter @Setter
    private Consumer<String> metaDataConsumer = l -> {};
    
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
        
        if (connection.getHeaderField(ICY_METAINT) != null) {
            log.debug("Found Icy Meta Interval header: {}", connection.getHeaderField(ICY_METAINT));
            int metaInterval = connection.getHeaderFieldInt(ICY_METAINT, 0);
            icyMetaFilterStream = new IcyMetaFilterStream(metaInterval, offsetFilterStream);
            icyMetaFilterStream.setMetaDataConsumer(m -> {metaDataConsumer.accept(m);});
            result = icyMetaFilterStream;
        }
        
        return result;
    }
}
