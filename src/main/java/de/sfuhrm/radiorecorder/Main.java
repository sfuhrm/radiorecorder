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

import de.sfuhrm.radiobrowser4j.Paging;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

    public static final String GITHUB_URL = "https://github.com/sfuhrm/radiorecorder";
    public static final String PROJECT = "Radio Recorder";

    /** Id for {@link ConsumerContext}. */
    private static int nextId = 1;

    private static RadioBrowser newRadioBrowser(Params params) {
        RadioBrowser browser = new RadioBrowser("https://de1.api.radio-browser.info/",
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
    private static List<Station> sanitize(List<String> urls, Params params) {
        List<Station> result = new ArrayList<>();

        RadioBrowser radioBrowser = newRadioBrowser(params);
        int limit = params.getStationLimit();
        for (String urlString : urls) {
            try {
                URL url = new URL(urlString); // parse the url
                Station s = new Station();
                s.setName("User-Suppplied URL");
                s.setUrl(url.toExternalForm());
                result.add(s);
            } catch (MalformedURLException ex) {
                log.debug("Parameter not an URL: "+urlString, ex);
                try {
                    UUID uuid = UUID.fromString(urlString);
                    List<Station> stations = radioBrowser.listStationsBy(SearchMode.BYUUID, uuid.toString()).collect(Collectors.toList());
                    result.addAll(stations);
                }
                catch (IllegalArgumentException e) {
                    log.debug("Parameter not an UUID: "+urlString, ex);
                    List<Station> stations = radioBrowser.listStationsBy(
                            Paging.at(0, limit),
                            SearchMode.BYNAME,
                            urlString);
                    result.addAll(stations);
                }
            }
        }
        return result;
    }

    private static ConsumerContext toConsumerContext(Params p, String url) throws MalformedURLException, UnsupportedEncodingException {
        URL myUrl = new URL(url);
        File dir = new File(p.getDirectory(), URLEncoder.encode(myUrl.getHost()+"/"+myUrl.getPath(), "UTF-8"));
        dir.mkdirs();
        return new ConsumerContext(nextId++, myUrl, dir, p);
    }

    @Value
    private static class CastItem {
        String title;
        String model;
        String address;
        String appTitle;
    }

    private static class MyListener implements ChromeCastsListener {
        private List<CastItem> discovered = new ArrayList<>();
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

        System.err.printf("Please wait %dms while discovering devices...%n", castSearchMillis);
        MyListener instance = new MyListener();
        ChromeCasts.registerListener(instance);
        ChromeCasts.startDiscovery();
        Thread.sleep(castSearchMillis);
        ChromeCasts.stopDiscovery();

        if (instance.discovered.isEmpty()) {
            System.out.println(NO_RESULTS);
            return;
        }

        ListHelper<CastItem> helper = new ListHelper<>(instance.discovered);
        helper.addColumn("Title", i -> i.getTitle());
        helper.addColumn("Model", i -> i.getModel());
        helper.addColumn("Address", i -> i.getAddress());
        helper.addColumn("App Title", i -> i.getAppTitle());
        helper.print(System.out);
    }

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

        if (params.getArguments() == null) {
            System.err.println("Please enter command line arguments (radio urls)");
            return;
        }

        Collection<Station> stations = sanitize(params.getArguments(), params);
        if (params.isPlay() && stations.size() > 1) {
            stations = stations.stream().limit(1).collect(Collectors.toList());
            System.err.println("Restricting to first station because playing.");
        }
        stations.stream().forEach(station -> {
            try {
                System.err.println(station);
                Runnable r = new RadioRunnable(toConsumerContext(params, station.getUrl()));
                Thread t = new Thread(r, station.getUrl());
                t.start();
            } catch (IOException ex) {
                log.warn("Could not start thread for station url "+station.getUrl(), ex);
            }
        });
    }

    private static void listMixers() {
        List<Mixer.Info> infoList = Arrays.asList(AudioSystem.getMixerInfo());

        if (infoList.isEmpty()) {
            System.out.println(NO_RESULTS);
            return;
        }

        ListHelper<Mixer.Info> helper = new ListHelper<>(infoList);
        helper.addColumn("Name", i -> i.getName());
        helper.addColumn("Description", i -> i.getDescription());
        helper.addColumn("Vendor", i -> i.getVendor());
        helper.print(System.out);
    }

    private static void listStations(List<String> names, Params params) {
        List<Station> stations = sanitize(names, params);

        if (stations.isEmpty()) {
            System.out.println(NO_RESULTS);
            return;
        }

        ListHelper<Station> helper = new ListHelper<>(stations);
        helper.addColumn("UUID", s -> s.getStationUUID().toString());
        helper.addColumn("Name", s -> s.getName());
        helper.addColumn("Codec", s -> s.getCodec());
        helper.addColumn("BR", s -> String.format("%d", s.getBitrate()));
        helper.addColumn("Tags", s -> s.getTags());

        helper.print(System.out);
    }

    private static String NO_RESULTS = "No results.";
}
