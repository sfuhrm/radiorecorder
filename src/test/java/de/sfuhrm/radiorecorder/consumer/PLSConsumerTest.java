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
package de.sfuhrm.radiorecorder.consumer;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PLSConsumerTest {

    @Test
    public void testReadUrls() {
        String testString =
 "[playlist]\n"+
 "NumberOfEntries=3\n"+ 
 "File1=http://streamexample.com:80\n"+
 "Title1=My Favorite Online Radio\n"+
 "Length1=-1\n"+
 "File2=http://example.com/song.mp3\n"+
 "Title2=Remote MP3\n"+
 "Length2=286\n"+ 
 "File3=D:\\Eigene Musik\\album.flac\n"+
 "Title3=Local album\n"+
 "Length3=3487\n"+
 "Version=2\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(testString.getBytes(Charset.forName("ASCII")));
        List<String> urls = PLSConsumer.readUrls(bais);
        assertEquals(Arrays.asList("http://streamexample.com:80", "http://example.com/song.mp3"), urls);
    }
}
