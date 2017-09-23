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
package de.sfuhrm.radiobrowser;

import java.util.List;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Stephan Fuhrmann
 */
public class RadioBrowserTest {
    
    private RadioBrowser browser;
    
    @Before
    public void create() {
        browser = new RadioBrowser(5000, "Test");
    }
    
    @Test
    public void listStations() {
        List<Station> stations = browser.listStations(0, 5);
        assertThat(stations, notNullValue());
        assertThat(stations.size(), is(5));
    }
    
    @Test
    public void listStationsBy() {
        List<Station> stations = browser.listStationsBy(0, 5, RadioBrowser.SearchMode.byname, "ding");
        assertThat(stations, notNullValue());
        assertThat(stations.size(), is(5));
        assertThat(stations.get(0).name.toLowerCase(), containsString("ding"));
    }    
}
