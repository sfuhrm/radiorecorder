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

import de.sfuhrm.radiorecorder.ConnectionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Filters icecast meta data out of the stream.
 * @see ConnectionHandler#configureIcecast(java.net.URLConnection) 
 * @author Stephan Fuhrmann
 */
@Slf4j
class IcyMetaFilterStream extends OffsetFilterStream {
       
    /** Byte interval in the stream to the next meta data block. */
    private final int metaInterval;
    
    /** Pattern for meta data String. */
    private final Pattern metaPattern;
    
    @Getter
    private String lastMetaData;
    
    @Getter @Setter
    private Consumer<String> metaDataConsumer = t -> {};
    
    IcyMetaFilterStream(int icyMetaInterval, InputStream inputStream) {
        super(inputStream);
        this.metaInterval = icyMetaInterval;
        metaPattern = Pattern.compile("StreamTitle='(.*)';");
    }
    
    private static int indexOf(byte[] array, byte findMe) {
        for (int i=0; i < array.length; i++) {
            if (array[i] == findMe) {
                return i;
            }
        }
        return -1;
    }
    
    private void readIcyMeta() throws IOException {
        int c;
        log.trace("Offset is {}, Icy Interval is {}", getOffset(), metaInterval);
        
        c = super.read();
        int length = (c & 0xff) * 16;
        
        byte metaData[] = new byte[length];
        int actually = super.read(metaData, 0, length);
        
        log.trace("Expected len {}, actual len {}", length, actually);
        
        // UTF-8 is probably wrong
        int firstZero = indexOf(metaData, (byte)0);
        int stringLen = firstZero != -1 ? firstZero : length;
        String meta = new String(metaData,0, stringLen, Charset.forName("UTF-8"));
        Matcher matcher = metaPattern.matcher(meta);
        if (matcher.matches()) {
            String currentMetaData = matcher.group(1);
            
            if (!currentMetaData.equals(lastMetaData)) {
                metaDataConsumer.accept(currentMetaData);
            }
            lastMetaData = currentMetaData;
            log.debug("Found metadata: {}", lastMetaData);
        } else {
            if (length != 0) {
                log.warn("No metadata found in stream, but had {} bytes (got: {})", length, meta);
            }
        }
        setOffset(0);
    }

    @Override
    public int read() throws IOException {
        if (getOffset() == metaInterval) {
            readIcyMeta();
        }
        
        int result = super.read();        
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result;
        if (getOffset() == metaInterval) {
            // we're right now on the offset
            readIcyMeta();
            result = super.read(b, off, len);
        } else {
            // bytes to go until next meta data
            int next = (int)(metaInterval - (getOffset() % metaInterval));

            if (len < next) {
                // the offset is not within the next 'len' bytes
                // go on, don't care about icy meta data
                result = super.read(b, off, len);                
            } else {
                // the offset is within the next 'len' bytes
                // first read next 'next' bytes and pretend we're not interested in more
                result = super.read(b, off, next);
            }
        }
        
        return result;
    }
}
