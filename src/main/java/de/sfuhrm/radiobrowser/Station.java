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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * @author Stephan Fuhrmann
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Station {
    public String id;
    public String name;
    public String ip;
    public String url;
    public String homepage;
    public String favicon;
    public String tags;
    public String country;
    public String state;
    public String language;
    public String votes;
    public String negativevotes;
    public String codec;
    public String bitrate;
    public String hls;
    public String lastcheckok;
    public String lastchecktime;
    public String lastcheckoktime;
    public String clicktimestamp;
    public String clickcount;
    public String clicktrend;
    public String lastchangetime;

    @Override
    public String toString() {
        return "Station{" + "name=" + name + ", url=" + url + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.id);
        hash = 31 * hash + Objects.hashCode(this.name);
        hash = 31 * hash + Objects.hashCode(this.url);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Station other = (Station) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        return true;
    }
    
    
    
}
