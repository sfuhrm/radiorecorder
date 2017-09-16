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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author fury
 */
class OffsetFilterStream extends FilterInputStream {
    
    @Getter @Setter
    private long offset;

    OffsetFilterStream(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public int read() throws IOException {
        int result = super.read(); //To change body of generated methods, choose Tools | Templates.
        if (result != -1) {
            offset++;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result != -1) {
            offset += result;
        }
        return result;
    }
}
