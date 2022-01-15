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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.sfuhrm.radiobrowser4j.SearchMode;
import de.sfuhrm.radiobrowser4j.Station;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

import javax.sound.sampled.AudioSystem;

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

    /** Read the URLs or names given and resolve them using {@link RadioBrowser}.
     * @param urls the input urls from the command line.
     * @param params the command line.
     * @return the sanitized URLs.
     */
    private static Collection<String> sanitize(List<String> urls, Params params) {
        Set<String> result = new HashSet<>();
        RadioBrowser browser = new RadioBrowser("https://de1.api.radio-browser.info/",
                params.getTimeout() * 1000,
                GITHUB_URL,
                params.getProxy() != null ? params.getProxy().toExternalForm() : null,
                null,
                null);
        int limit = params.getStationLimit();
        for (String urlString : urls) {
            try {
                new URL(urlString); // parse the url
                result.add(urlString);
            } catch (MalformedURLException ex) {
                log.debug("URL not valid "+urlString+", will try to lookup", ex);
                List<Station> stations = browser.listStationsBy(
                        Paging.at(0, limit),
                        SearchMode.BYNAME,
                        urlString);
                result.addAll(stations.stream().map(Station::getUrl).collect(Collectors.toList()));
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

    private static class MyListener implements ChromeCastsListener {
        @Override
        public void newChromeCastDiscovered(ChromeCast chromeCast) {
            System.out.printf("%s - %s%n", chromeCast.getTitle(), chromeCast.getModel());
        }

        @Override
        public void chromeCastRemoved(ChromeCast chromeCast) {
        }
    }

    private static void listCastDevices() throws InterruptedException, IOException {
        ChromeCasts.registerListener(new MyListener());
        ChromeCasts.startDiscovery();
        Thread.sleep(5000);
        ChromeCasts.stopDiscovery();
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

        if (params.isListMixers()) {
            listMixers();
            return;
        }

        if (params.getArguments() == null) {
            System.err.println("Please enter command line arguments (radio urls)");
            return;
        }

        Collection<String> stations = sanitize(params.getArguments(), params);
        if (params.isPlay() && stations.size() > 1) {
            stations = stations.stream().limit(1).collect(Collectors.toList());
            System.err.println("Restricting to first station because playing.");
        }
        stations.stream().forEach(url -> {
            try {
                System.err.println(url);
                Runnable r = new RadioRunnable(toConsumerContext(params, url));
                Thread t = new Thread(r, url);
                t.start();
            } catch (IOException ex) {
                log.warn("Could not start thread for url "+url, ex);
            }
        });
    }

    private static void listMixers() {
        Arrays.stream(AudioSystem.getMixerInfo()).forEach(mi ->
                System.out.println("Mixer name: " + mi.getName() + ", Description: " + mi.getDescription() + ", Vendor: " + mi.getVendor()));
    }
}
