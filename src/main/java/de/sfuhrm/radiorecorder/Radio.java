package de.sfuhrm.radiorecorder;

import de.sfuhrm.radiobrowser4j.Station;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** An internet radio station for use within the application.
 * @see Station
* */
@Getter @Setter
@Slf4j
@ToString
public class Radio {
    /** The station name. */
    private String name;

    /** The station URL. */
    private URL url;

    /** The station UUID in radio browser. */
    private UUID uuid;

    /** The station favicon URL (optional). */
    private URL favIconUrl;

    /** The associated codec. */
    private String codec;

    /** The bitrate in kbits per second. */
    private int bitrate;

    /** The tags. */
    private List<String> tags = Collections.emptyList();

    /** Converts a Station to a Radio object.
     * @param s the station object to convert.
     * @return the radio object to use in the application.
     * @throws IllegalArgumentException if the station object contains malformed data, for
     * example illegal URL strings.
     * */
    static Radio fromStation(Station s) {
        Radio r = new Radio();
        r.setName(s.getName());
        if (s.getUrl() != null) {
            try {
                r.setUrl(new URL(s.getUrl()));
            } catch (MalformedURLException e) {
                log.error("Error parsing station URL '" + s.getUrl() + "'", e);
                throw new IllegalArgumentException(e);
            }
        }
        r.setUuid(s.getStationUUID());
        if (s.getFavicon() != null && ! s.getFavicon().isEmpty()) {
            try {
                r.setFavIconUrl(new URL(s.getFavicon()));
            } catch (MalformedURLException e) {
                log.warn("Error parsing station favicon URL '" + s.getFavicon() + "'", e);
            }
        }
        r.setCodec(s.getCodec());
        r.setBitrate(r.getBitrate());
        r.setTags(new ArrayList<>(r.getTags()));
        return r;
    }
}
