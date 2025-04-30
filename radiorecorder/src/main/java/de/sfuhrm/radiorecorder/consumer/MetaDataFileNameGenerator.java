package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.Params;
import de.sfuhrm.radiorecorder.Radio;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import lombok.NonNull;
import org.apache.commons.text.StringSubstitutor;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Generates song file names out of received {@link MetaData}. */
class MetaDataFileNameGenerator {
    /**
     * @see Params#getSongnameFormat() ()
     * */
    private final String fileNameFormat;

    /**
     * @see Params#getDirectory()
     */
    private final Path targetDirectory;

    /**
     * Generate names only with metadata.
     */
    private final boolean requireMetaData;

    MetaDataFileNameGenerator(@NonNull  String fileNameFormat, @NonNull ConsumerContext consumerContext, boolean requireMetaData) {
        this.fileNameFormat = fileNameFormat;
        this.targetDirectory = consumerContext.getTargetDirectory();
        this.requireMetaData = requireMetaData;
    }

    static String sanitizeFileName(String in) {
        return in.replaceAll("[/:\\|?$\\\\]", "_");
    }

    /** Get meta data field falling back to a default.
     * @param metaData meta data to get field from or {@code null}.
     * @param getter meta data field getter.
     * @param fallback fallback value of metaData or the field itself is {@code null}.
     * @return filesystem-sanitized metadata field content or fallback if field is not given.
     * */
    private static String getMetaDataField(MetaData metaData, Function<MetaData, Optional<String>> getter, String fallback) {
        String result = fallback;
        if (metaData != null) {
            Optional<String> value = getter.apply(metaData);
            if (value != null && value.isPresent()) {
                result = value.get();
            }
        }
        return sanitizeFileName(result);
    }

    /** Gets a string substitutor for file name generation. */
    private static StringSubstitutor newStringSubstitutor(Radio radio, MetaData metaData, MimeType mimeTypeNullable) {
        String unknown = "unknown";
        Map<String, String> values = new HashMap<>();

        // icy metadata
        values.put("artist", getMetaDataField(metaData, MetaData::getArtist, unknown));
        values.put("title", getMetaDataField(metaData, MetaData::getTitle, unknown));

        values.put("stationUrl", getMetaDataField(metaData, MetaData::getStationUrl, unknown));
        values.put("stationName", getMetaDataField(metaData, MetaData::getStationName, unknown));
        values.put("stationHost", getMetaDataField(metaData,
                m -> m.getStationUrl().map(url -> URI.create(url).getHost()), unknown));

        // radio metadata
        values.put("radioName", radio.getName());
        values.put("radioHost", radio.getUri().getHost());

        values.put("index", getMetaDataField(metaData, m -> m.getIndex().map(intValue -> String.format("%03d", intValue)), unknown));
        values.put("suffix", suffixFromContentType(mimeTypeNullable));

        StringSubstitutor result = new StringSubstitutor(values);
        return result;
    }

    /**
     * Get the file name derived from the received metadata.
     * @param metaData nullable meta data from the stream.
     * @param contentType content type from the stream.
     * @return the path, if there is metadata, or empty.
     */
    Optional<Path> getFileFrom(Radio radio, MetaData metaData, MimeType contentType) {
        Optional<Path> result = Optional.empty();

        if (metaData != null || !requireMetaData) {
            StringSubstitutor substitutor = newStringSubstitutor(radio, metaData, contentType);
            String targetName = substitutor.replace(fileNameFormat);
            result = Optional.of(targetDirectory.resolve(targetName));
        }
        return result;
    }

    /**
     * Calculate the file suffix.
     */
    private static String suffixFromContentType(MimeType contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.getSuffix();
    }
}
