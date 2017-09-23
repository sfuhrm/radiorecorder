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

import de.sfuhrm.radiobrowser.RadioBrowser;
import de.sfuhrm.radiobrowser.Station;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

/**
 * The main class that gets executed from command line.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class Main {

    public final static String GITHUB_URL = "https://github.com/sfuhrm";
    public final static String PROJECT = "Radio Recorder";
    
    /** Id for {@link ConsumerContext#id}. */
    private static int nextId = 1;
    
    private static List<String> sanitize(List<String> urls, Params params) {
        List<String> result = new ArrayList<>();
        RadioBrowser browser = new RadioBrowser(params.getTimeout() * 1000, PROJECT);
        
        int limit = 10;
        for (String urlString : urls) {
            try {
                new URL(urlString); // parse the url
                result.add(urlString);
            } catch (MalformedURLException ex) {
                log.debug("URL not valid "+urlString+", will try to lookup", ex);
                List<Station> stations = browser.listStationsBy(0, limit, RadioBrowser.SearchMode.byname, urlString);
                result.addAll(stations.stream().map(s -> s.url).collect(Collectors.toList()));
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
            System.out.printf("%s - %s\n", chromeCast.getTitle(), chromeCast.getModel());
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
        
        if (params.getArguments() == null) {
            System.err.println("Please enter command line arguments (radio urls)");
            return;
        }

        List<String> stations = sanitize(params.getArguments(), params);
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
}
