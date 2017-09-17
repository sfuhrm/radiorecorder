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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * The main class that gets executed from command line.
 *
 * @author Stephan Fuhrmann
 */
@Slf4j
public class Main {

    private static ConsumerContext toConsumerContext(Params p, String url) throws MalformedURLException, UnsupportedEncodingException {
        URL myUrl = new URL(url);        
        File dir = new File(p.getDirectory(), URLEncoder.encode(myUrl.getHost()+"/"+myUrl.getPath(), "UTF-8"));
        dir.mkdirs();
        return new ConsumerContext(myUrl, dir, p);
    }
    
    public static void main(String[] args) throws IOException {
        Params params = Params.parse(args);
        if (params == null) {
            return;
        }
        
        if (params.getArguments() == null) {
            System.err.println("Please enter command line arguments (radio urls)");
            return;
        }

        params.getArguments().stream().forEach(url -> {
            File dir = null;
            try {
                Runnable r = new RadioRunnable(toConsumerContext(params, url));
                Thread t = new Thread(r, url);
                t.start();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        // do something good here
    }
}
