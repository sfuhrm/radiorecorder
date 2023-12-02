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

import de.sfuhrm.radiobrowser4j.EndpointDiscovery;
import de.sfuhrm.radiobrowser4j.Paging;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import de.sfuhrm.radiobrowser4j.SearchMode;
import de.sfuhrm.radiobrowser4j.Station;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * The main class that gets executed from command line.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class Main {

    /** The URL of the GitHub project. */
    public static final String GITHUB_URL = "https://github.com/sfuhrm/radiorecorder";

    /** The name of the application. */
    public static final String PROJECT = "Radio Recorder";

    /** Id for {@link ConsumerContext}. */
    private static int nextId = 1;

    private static RadioBrowser newRadioBrowser(Params params) throws IOException {
        EndpointDiscovery endpointDiscovery = new EndpointDiscovery(GITHUB_URL);
        Optional<String> endpoint = endpointDiscovery.discover();

        if (! endpoint.isPresent()) {
            throw new Error("Radiobrowser endpoint discovery failed");
        }

        RadioBrowser browser = new RadioBrowser(endpoint.get(),
                params.getTimeout() * 1000,
                GITHUB_URL,
                params.getProxy() != null ? params.getProxy().toExternalForm() : null,
                null,
                null);
        return browser;
    }

    /** Read the URLs or names given and resolve them using {@link RadioBrowser}.
     * @param urls the input urls from the command line.
     * @param params the command line.
     * @return the sanitized URLs.
     */
    private static List<Radio> sanitize(List<String> urls, Params params) throws IOException {
        List<Radio> result = new ArrayList<>();

        RadioBrowser radioBrowser = newRadioBrowser(params);
        int limit = params.getStationLimit();
        for (String urlString : urls) {
            try {
                URL url = new URL(urlString); // parse the url
                Radio s = new Radio();
                s.setName("User-Suppplied URL");
                s.setUrl(url);
                result.add(s);
            } catch (MalformedURLException ex) {
                log.debug("Parameter not an URL: "+urlString, ex);
                try {
                    UUID uuid = UUID.fromString(urlString);
                    List<Station> stations = radioBrowser.listStationsBy(SearchMode.BYUUID, uuid.toString()).collect(Collectors.toList());
                    List<Radio> radios = stations.stream().map(Radio::fromStation).collect(Collectors.toList());
                    result.addAll(radios);
                }
                catch (IllegalArgumentException e) {
                    log.debug("Parameter not an UUID: "+urlString, ex);
                    List<Station> stations = radioBrowser.listStationsBy(
                            Paging.at(0, limit),
                            SearchMode.BYNAME,
                            urlString);
                    List<Radio> radios = stations.stream().map(Radio::fromStation).collect(Collectors.toList());
                    result.addAll(radios);
                }
            }
        }
        return result;
    }

    private static ConsumerContext toConsumerContext(Params p, Radio radio) throws MalformedURLException {
        return new ConsumerContext(nextId++, radio, p);
    }

    @Value
    private static class CastItem {
        String title;
        String model;
        String address;
        String appTitle;
    }

    private static class MyListener implements ChromeCastsListener {
        private final List<CastItem> discovered = new ArrayList<>();
        @Override
        public void newChromeCastDiscovered(ChromeCast chromeCast) {
            CastItem castItem = new CastItem(chromeCast.getTitle(), chromeCast.getModel(), chromeCast.getAddress(), chromeCast.getAppTitle());
            synchronized (discovered) {
                discovered.add(castItem);
            }
        }

        @Override
        public void chromeCastRemoved(ChromeCast chromeCast) {
        }
    }

    private static void listCastDevices() throws InterruptedException, IOException {
        int castSearchMillis = 5000;

        log.info("Please wait {}ms while discovering devices...", castSearchMillis);
        MyListener instance = new MyListener();
        ChromeCasts.registerListener(instance);
        ChromeCasts.startDiscovery();
        Thread.sleep(castSearchMillis);
        ChromeCasts.stopDiscovery();

        if (instance.discovered.isEmpty()) {
            log.warn(NO_RESULTS);
            return;
        }

        ListHelper<CastItem> helper = new ListHelper<>(instance.discovered);
        helper.addColumn("Title", CastItem::getTitle);
        helper.addColumn("Model", CastItem::getModel);
        helper.addColumn("Address", CastItem::getAddress);
        helper.addColumn("App Title", CastItem::getAppTitle);
        helper.print(System.out);
    }

    /** Main method of the program.
     * @param args command line arguments.
     * @see Params
     * @throws IOException if there's a problem with IO
     * @throws InterruptedException when there's an unexpected interrupt
     * */
    public static void main(String[] args) throws IOException, InterruptedException {
        Params params = Params.parse(args);
        if (params == null) {
            return;
        }

        if (params.isListCast()) {
            listCastDevices();
            return;
        }

        if (params.isListStation()) {
            listStations(params.getArguments(), params);
            return;
        }

        if (params.isListMixers()) {
            listMixers();
            return;
        }

        Collection<Radio> radios = sanitize(params.getArguments(), params);
        if (params.isPlay() && radios.isEmpty()) {
            log.warn("No search results for the search arguments: {}", params.getArguments());
            return;
        }
        if (params.isPlay() && radios.size() > 1) {
            radios = radios.stream().limit(1).collect(Collectors.toList());
            log.warn("Restricting to first station because playing.");
        }

        List<Thread> threadList = new ArrayList<>();
        radios.stream().forEach(radio -> {
            try {
                log.info("Starting radio: {}", radio);
                RadioRunnable r = new RadioRunnable(toConsumerContext(params, radio));
                Thread t = new Thread(r, "Radio " + radio.getUuid());
                threadList.add(t);
                t.start();
            } catch (IOException ex) {
                log.warn("Could not start thread for station url "+radio.getUrl(), ex);
            }
        });

        // wait for finish
        log.info("Waiting for background processes to finish");
        joinThreads(threadList);
    }

    private static void joinThreads(List<Thread> threadList) {
        threadList.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void listMixers() {
        List<Mixer.Info> infoList = Arrays.asList(AudioSystem.getMixerInfo());

        if (infoList.isEmpty()) {
            log.warn(NO_RESULTS);
            return;
        }

        ListHelper<Mixer.Info> helper = new ListHelper<>(infoList);
        helper.addColumn("Name", Mixer.Info::getName);
        helper.addColumn("Description", Mixer.Info::getDescription);
        helper.addColumn("Vendor", Mixer.Info::getVendor);
        helper.print(System.out);
    }

    private static void listStations(List<String> names, Params params) throws IOException {
        List<Radio> radios = sanitize(names, params);

        if (radios.isEmpty()) {
            log.warn(NO_RESULTS);
            return;
        }

        ListHelper<Radio> helper = new ListHelper<>(radios);
        helper.addColumn("UUID", s -> s.getUuid().toString());
        helper.addColumn("Name", Radio::getName);
        helper.addColumn("Codec", Radio::getCodec);
        helper.addColumn("BR", s -> String.format("%d", s.getBitrate()));
        helper.addColumn("Tags", s -> s.getTags().toString());

        helper.print(System.out);
    }

    private static final String NO_RESULTS = "No results.";
}
