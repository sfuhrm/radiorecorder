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
import de.sfuhrm.radiorecorder.RadioException;
import static de.sfuhrm.radiorecorder.RadioRunnable.BUFFER_SIZE;
import de.sfuhrm.radiorecorder.http.HttpConnection;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;
import su.litvak.chromecast.api.v2.MediaStatus;

/**
 * Plays a stream using a named Chrome Cast device.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamCastConsumer extends MetaDataConsumer implements Consumer<HttpConnection> {

    /** The ID of the default media receiver app.
     */
    public static final String APP_ID = "CC1AD845";

    /** Async communication of the chromecast discovered. */
    private final ArrayBlockingQueue<ChromeCast> arrayBlockingQueue;

    /** The chrome cast discovered. */
    private ChromeCast chromeCast = null;

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
        }
    }


    public StreamCastConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        arrayBlockingQueue = new ArrayBlockingQueue<>(1);
    }

    @Override
    protected void __accept(HttpConnection t, InputStream inputStream) {
        try {
            getStreamMetaData().setMetaDataConsumer(new ConsoleMetaDataConsumer());

            ChromeCasts.registerListener(new MyChromeCastsListener());
            ChromeCasts.startDiscovery();

            System.err.println("Waiting for discovery");
            log.debug("Waiting for chromecast {} to be discovered", getContext().getCastReceiver());
            chromeCast = arrayBlockingQueue.take();

            log.debug("Found chromecast {}", chromeCast);

            chromeCast.connect();
            log.debug("Connected to chromecast {}", chromeCast);

            Application app = chromeCast.launchApp(APP_ID);
            chromeCast.setApplication(Main.PROJECT);
            chromeCast.setName("My Name");
            MediaStatus mediaStatus = chromeCast.load(Main.PROJECT, null, t.getURL().toExternalForm(), t.getContentType());

            log.debug("Loaded content to chromecast {}", chromeCast.getTitle());

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            Thread shutdown = new Thread(this::cleanup);
            Runtime.getRuntime().addShutdownHook(shutdown);

            try {
            // this is a second stream just to display the meta data
            while (-1 != (length = inputStream.read(buffer))) {
                log.trace("Read {} bytes", length);
            }
            Runtime.getRuntime().removeShutdownHook(shutdown);
            }
            catch (IOException ioe) {
                log.warn("Error reading stream", ioe);
                throw new RadioException(true, ioe);
            }

        } catch (GeneralSecurityException | InterruptedException | IOException  ex) {
            log.warn("Chromecast problem", ex);
            throw new RadioException(false, ex);
        }
        finally {
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
                chromeCast = null;
            }
            ChromeCasts.stopDiscovery();
            log.debug("Stopped discovery");
        } catch (IOException ex) {
        }
    }
}
