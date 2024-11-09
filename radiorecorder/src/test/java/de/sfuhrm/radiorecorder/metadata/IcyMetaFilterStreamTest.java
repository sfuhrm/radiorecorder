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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Mockito based test for {@link IcyMetaFilterStream}.
 *
 * @author Stephan Fuhrmann
 */
public class IcyMetaFilterStreamTest {

    private static byte[] streamTitle(String in) {
        String test = "StreamTitle='"+in+"';";
        byte[] data = test.getBytes();
        int blocks = (data.length+15) / 16;
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.put((byte)blocks);
        bb.put(data);
        bb.put(new byte[blocks*16 - data.length]);
        byte[] result = new byte[1 + blocks*16];
        bb.rewind();
        bb.get(result);
        return result;
    }

    @Test
    void readWithByteArrayInputStream() throws IOException {    
        Random rand = new Random();
        ByteBuffer bb = ByteBuffer.allocate(256);
        
        byte[] b = new byte[4];
        rand.nextBytes(b);
        bb.put(b) // 4 fill bytes to skip
        .put(streamTitle("hello"))
        .put(b) // 8
        .put((byte)0)
        .put(b) // 12
        .put(streamTitle("world"))
        .put(b); // 16
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
                
        IcyMetaFilterStream icy = new IcyMetaFilterStream(4, bais);        
        assertNull(icy.getLastMetaData());
        byte[] r = new byte[4];
        int c;
        
        c = icy.read(r); // 4        
        assertEquals(4, c);
        assertArrayEquals(b, r);        

        c = icy.read(r); // 8
        assertEquals(4, c);
        assertArrayEquals(b, r);        
        assertEquals("hello", icy.getLastMetaData());

        icy.read(new byte[4]); // 10
        assertEquals("hello", icy.getLastMetaData());        
        icy.read(new byte[4]); // 14
        icy.read(new byte[8]); // 18
        assertEquals("world", icy.getLastMetaData());        
    }
    
}
