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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

/**
 * Plays a stream using the Java Media Framework API.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class StreamCastConsumer extends MetaDataConsumer implements Consumer<URLConnection> {

    private ArrayBlockingQueue<ChromeCast> arrayBlockingQueue;
    
    public final static String APP_ID = "RadioRecorder";
    
    public StreamCastConsumer(ConsumerContext consumerContext) {
        super(consumerContext);
        arrayBlockingQueue = new ArrayBlockingQueue<>(1);
    }

    @Override
    protected void __accept(URLConnection t, InputStream inputStream) {
        try {
            getStreamMetaData().setMetaDataConsumer(m -> {
                System.err.println(m);
            });
            
            ChromeCasts.registerListener(new ChromeCastsListener() {
                @Override
                public void newChromeCastDiscovered(ChromeCast chromeCast) {
                    if (chromeCast.getTitle().equalsIgnoreCase(getContext().getCastReceiver())) {
                        try {
                            log.debug("Found chromecast {}", chromeCast.getTitle());
                            arrayBlockingQueue.put(chromeCast);
                            log.debug("Posted chromecast {}", chromeCast.getTitle());
                            ChromeCasts.stopDiscovery();
                        } catch (IOException | InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    } else {
                        log.debug("Ignoring chromecast {}", chromeCast.getTitle());                        
                    }
                }

                @Override
                public void chromeCastRemoved(ChromeCast chromeCast) {
                }
            });
            ChromeCasts.startDiscovery();
            
            log.debug("Waiting for chromecast {} to be discovered", getContext().getCastReceiver());
            ChromeCast chromeCast = arrayBlockingQueue.take();
            
            log.debug("Found chromecast {}", chromeCast);
            
            chromeCast.connect();
            log.debug("Connected to chromecast {}", chromeCast);
            
            chromeCast.launchApp(APP_ID);
            log.debug("App with id {} launched to chromecast {}", APP_ID, chromeCast);
            
            chromeCast.load(Main.PROJECT, null, t.getURL().toExternalForm(), t.getContentType());
            log.debug("Loaded content to chromecast {}", chromeCast);
        } catch (GeneralSecurityException | InterruptedException | IOException ex) {
            log.warn("Chromecast problem", ex);
        }
    }
}
