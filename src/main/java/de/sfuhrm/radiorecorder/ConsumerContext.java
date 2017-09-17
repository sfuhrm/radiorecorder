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
package de.sfuhrm.radiorecorder;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import lombok.Getter;

/**
 * Immutable context common to all consumers.
 * Holds the data necessary to record or play one stream from one
 * radio station.
 * @author Stephan Fuhrmann
 */
public class ConsumerContext {
    
    @Getter
    private final URL url;

    @Getter
    private final File directory;
    
    private final Params params;
    
    public ConsumerContext(URL url, File directory, Params params) {
        this.url = Objects.requireNonNull(url);
        this.directory = Objects.requireNonNull(directory);
        this.params = Objects.requireNonNull(params);
    }
    
    /** Get the read/connect timeout in millis. 
     * @return the timeout in milliseconds.
     */
    public int getTimeout() {
        return params.getTimeout() * 1000;
    }

    /** Get minimum free bytes.
     * @return minimum number of free bytes on disk.
     */
    public long getMinFree() {
        return params.getMinimumFree() * 1024 * 1024;
    }
    
    /** Whether to play or store. */
    public boolean isPlaying() {
        return params.isPlay();
    }
    
    /** Whether to name the files after the metadata retrieved. */
    public boolean isSongNames() {
        return params.isSongNames();
    }
}
