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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * Immutable context common to all consumers.
 * Holds the data necessary to record or play one stream from one
 * radio station.
 * @author Stephan Fuhrmann
 */
@Slf4j
public class ConsumerContext {

    @Getter
    private final int id;

    @Getter
    private final Radio radio;

    @Getter
    private final URL url;

    private final Params params;

    public ConsumerContext(int id, Radio radio, Params params) throws MalformedURLException {
        this.id = id;
        this.radio = radio;
        this.url = radio.getUrl();
        this.params = Objects.requireNonNull(params);
    }

    /** Get the read/connect timeout in millis.
     * @return the timeout in milliseconds.
     */
    public int getTimeout() {
        return params.getTimeout() * 1000;
    }

    /** Get the directory to write files to.
     * @return directory to write files to.
     */
    public File getTargetDirectory() {
        return params.getDirectory();
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
    public Optional<Long> getAbortAfterFileLength() {
        if (params.getAbortAfterKB() == null) {
            return Optional.empty();
        }
        return Optional.of(params.getAbortAfterKB() * 1024);
    }

    /** Get the amount of bytes after which to abort.
     * @return optional maximum of milliseconds to record.
     */
    public Optional<Long> getAbortAfterDuration() {
        if (params.getAbortAfterDuration() == null) {
            return Optional.empty();
        }
        return Optional.of(Params.toMillis(params.getAbortAfterDuration()));
    }

    /** Whether to play or store.
     * @return {@code true} if playing was requested on the command line,
     * {@code false} otherwise.
     * */
    public boolean isPlaying() {
        return params.isPlay();
    }

    /** Whether to name the files after the metadata retrieved.
     * @return {@code true} if metadata naming was requested on the command line,
     * {@code false} otherwise.
     * */
    public boolean isSongNames() {
        return params.isSongNames();
    }

    /** Reconnect forever.
     * @return {@code true} if reconnecting was requested on the command line,
     * {@code false} otherwise.
     * */
    public boolean isReconnect() {
        return params.isReconnect();
    }

    /** The cast device to cast to.
     * @return the name of the chrome cast receiver to play on.
     * */
    public String getCastReceiver() {
        return params.getCastReceiver();
    }

    /** The mixer to play on.
     * @return the mixer info to play on, or {@code null} if
     * the default is ok.
     * */
    public Mixer.Info getMixerInfo() {
        if (params.getMixer() != null) {
            Optional<Mixer.Info> optionalInfo = Arrays.stream(AudioSystem.getMixerInfo()).filter(mi -> mi.getName().equals(params.getMixer())).findFirst();
            if (optionalInfo.isPresent()) {
                return optionalInfo.get();
            } else {
                log.warn("No mixer info for command line mixer argument: {}. Falling back to default.", params.getMixer());
            }
        }
        return null;
    }

    /** The client type to use.
     * @return the HTTP client type requested in the command line.
     * */
    public HttpConnectionBuilderFactory.HttpClientType getHttpClient() {
        return params.getHttpClientType();
    }

    /** The HTTP proxy to use or NULL.
     * @return the HTTP proxy requested in the command line.
     * */
    public URL getProxy() {
        return params.getProxy();
    }
}
