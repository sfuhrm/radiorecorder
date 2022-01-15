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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import lombok.Getter;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * Immutable context common to all consumers.
 * Holds the data necessary to record or play one stream from one
 * radio station.
 * @author Stephan Fuhrmann
 */
public class ConsumerContext {

    @Getter
    private final int id;

    @Getter
    private final URL url;

    @Getter
    private final File directory;

    private final Params params;

    public ConsumerContext(int id, URL url, File directory, Params params) {
        this.id = id;
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
        return params.getMinimumFreeMegs() * 1024 * 1024;
    }

    /** Get the amount of bytes after which to abort.
     * @return optional maximum of bytes after which to abort writing to write to disk.
     */
    public Optional<Long> getAbortAfter() {
        if (params.getAbortAfterKilo() == null) {
            return Optional.empty();
        }
        return Optional.of(params.getAbortAfterKilo() * 1024);
    }

    /** Whether to play or store. */
    public boolean isPlaying() {
        return params.isPlay();
    }

    /** Whether to name the files after the metadata retrieved. */
    public boolean isSongNames() {
        return params.isSongNames();
    }

    /** Reconnect forever. */
    public boolean isReconnect() {
        return params.isReconnect();
    }

    /** The cast device to cast to. */
    public String getCastReceiver() {
        return params.getCastReceiver();
    }

    /** The mixer to play on. */
    public Mixer.Info getMixerInfo() {
        if (params.getMixer() != null) {
            Optional<Mixer.Info> optionalInfo = Arrays.stream(AudioSystem.getMixerInfo()).filter(mi -> mi.getName().equals(params.getMixer())).findFirst();
            if (optionalInfo.isEmpty()) {
                System.err.println("No mixer info " + params.getMixer());
            }
            if (optionalInfo.isPresent()) {
                return optionalInfo.get();
            }
        }
        return null;
    }

    /** The client type to use. */
    public HttpConnectionBuilderFactory.HttpClientType getHttpClient() {
        return params.getHttpClientType();
    }

    /** The HTTP proxy to use or NULL. */
    public URL getProxy() {
        return params.getProxy();
    }
}
