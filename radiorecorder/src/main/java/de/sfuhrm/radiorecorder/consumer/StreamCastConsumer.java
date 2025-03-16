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

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.Main;
import de.sfuhrm.radiorecorder.Radio;
import de.sfuhrm.radiorecorder.RadioException;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastException;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;
import su.litvak.chromecast.api.v2.MediaStatus;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;

/**
 * Plays a stream using a named Chrome Cast device.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamCastConsumer extends MetaDataConsumer implements Consumer<HttpConnection> {

    /**
     * The ID of the default media receiver app.
     */
    public static final String APP_ID = "CC1AD845";

    /** The application string to use for this application. */
    private static final String CHROMECAST_APPLICATION = Main.PROJECT;

    /**
     * Async communication of the chromecast discovered.
     */
    private final ArrayBlockingQueue<ChromeCast> arrayBlockingQueue;

    /**
     * The chrome cast discovered.
     */
    private ChromeCast chromeCast = null;

    private MediaStatus lastMediaStatus;

    private static final long TRACK_MEDIASTATUS_EVERY_MS = 1000L;

    private class MyChromeCastsListener implements ChromeCastsListener {

        @Override
        public void newChromeCastDiscovered(ChromeCast chromeCast) {
            if (chromeCast.getTitle().equalsIgnoreCase(getContext().getCastReceiver())) {
                try {
                    log.debug("Found chromecast {}", chromeCast.getTitle());
                    arrayBlockingQueue.put(chromeCast);
                    log.debug("Posted chromecast {}", chromeCast.getTitle());
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                log.debug("Ignoring chromecast {}", chromeCast.getTitle());
            }
        }

        @Override
        public void chromeCastRemoved(ChromeCast chromeCast) {
            log.debug("Removed chromecast {}", chromeCast.getTitle());
        }
    }

    /** Constructor.
     * @param consumerContext the context to work in.
     * */
    public StreamCastConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        arrayBlockingQueue = new ArrayBlockingQueue<>(1);
    }

    private boolean trackMediaStatusShallExit(String application, MediaStatus mediaStatus) {
        boolean result = false;
        if (mediaStatus == null) {
            return false;
        }

        if (lastMediaStatus == null || mediaStatus.playerState != lastMediaStatus.playerState) {
            log.info("Player state changed to {}", mediaStatus.playerState);
        }

        if (! CHROMECAST_APPLICATION.equals(application)) {
            log.info("Application changed from {} to {}, exiting",
                    CHROMECAST_APPLICATION,
                    application);
            result = true;
        }

        if (lastMediaStatus != null &&
            lastMediaStatus.equals(MediaStatus.PlayerState.PLAYING) &&
            mediaStatus.playerState.equals(MediaStatus.PlayerState.IDLE)) {
            log.info("Player state is {}. reason: {}, exiting",
                    mediaStatus.playerState,
                    mediaStatus.idleReason);
            result = true;
        }

        lastMediaStatus = mediaStatus;
        return result;
    }

    @Override
    protected void __accept(HttpConnection t, InputStream inputStream) {
        try {

            getStreamMetaData().setMetaDataConsumer(new ConsoleMetaDataConsumer());

            ChromeCasts.registerListener(new MyChromeCastsListener());
            ChromeCasts.startDiscovery();

            log.info("Waiting for chromecast {} to be discovered", getContext().getCastReceiver());
            chromeCast = arrayBlockingQueue.take();

            log.info("Found chromecast {}", chromeCast);

            chromeCast.connect();
            log.info("Connected to chromecast {}", chromeCast);

            Radio radio = getContext().getRadio();
            Application app = chromeCast.launchApp(APP_ID);
            chromeCast.setApplication(CHROMECAST_APPLICATION);
            chromeCast.setName(radio.getName());
            String application = chromeCast.getApplication();
            MediaStatus mediaStatus = chromeCast.load(Main.PROJECT + ": " + radio.getName(),
                    radio.getFavIconUrl() != null ? radio.getFavIconUrl().toASCIIString() : null,
                    t.getURI().toASCIIString(),
                    t.getContentType());
            boolean shallExit;
            shallExit = trackMediaStatusShallExit(application, mediaStatus);
            if (shallExit) {
                return;
            }

            log.debug("Loaded content to chromecast {}", chromeCast.getTitle());

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            Thread shutdown = new Thread(this::cleanup);
            Runtime.getRuntime().addShutdownHook(shutdown);

            long lastTrack = System.currentTimeMillis();
            try {
                // this is a second stream just to display the metadata
                while (chromeCast != null && -1 != (length = inputStream.read(buffer))) {
                    log.trace("Read {} bytes", length);

                    if (System.currentTimeMillis() - lastTrack > TRACK_MEDIASTATUS_EVERY_MS) {
                        lastTrack = System.currentTimeMillis();
                        synchronized (this) {
                            if (chromeCast != null) {
                                try {
                                    application = chromeCast.getApplication();
                                    mediaStatus = chromeCast.getMediaStatus();
                                    shallExit = trackMediaStatusShallExit(application, mediaStatus);
                                } catch (ChromeCastException e) {
                                    shallExit = true;
                                    log.warn("Chrome cast exception caught", e);
                                }
                                if (shallExit) {
                                    log.info("Media status said shall exit");
                                    return;
                                }
                            } else {
                                log.info("Chromecast absence said shall exit");
                                return;
                            }
                        }
                    }
                }
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdown);
                } catch (IllegalStateException e) {
                    // shotdown already in progress, ignore
                }
            } catch (IOException ioe) {
                log.warn("Error reading stream", ioe);
                throw new RadioException(true, ioe);
            }

        } catch (GeneralSecurityException | InterruptedException | IOException ex) {
            log.warn("Chromecast problem", ex);
            throw new RadioException(false, ex);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (chromeCast != null && chromeCast.isConnected()) {
                if (chromeCast.isAppRunning(APP_ID)) {
                    chromeCast.stopApp();
                }
                chromeCast.disconnect();
                log.debug("Disconnected from chromecast {}", chromeCast.getTitle());
                synchronized(this) {
                    chromeCast = null;
                }
            }
            ChromeCasts.stopDiscovery();
            log.debug("Stopped discovery");
        } catch (IOException ex) {
            log.warn("Problem disconnecting chromecast", ex);
        }
    }
}
