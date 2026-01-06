package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.Params;
import de.sfuhrm.radiorecorder.Radio;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates song file names out of received {@link MetaData}. */
@Slf4j
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
     * @see ConsumerContext#getId()
     */
    private final int consumerId;

    /**
     * Generate names only with metadata.
     */
    private final boolean requireMetaData;

    MetaDataFileNameGenerator(@NonNull  String fileNameFormat, @NonNull ConsumerContext consumerContext, boolean requireMetaData) {
        this.fileNameFormat = fileNameFormat;
        this.targetDirectory = consumerContext.getTargetDirectory();
        this.consumerId = consumerContext.getId();
        this.requireMetaData = requireMetaData;
    }

    String sanitizeFileName(String in) {
        String sanitized = in;

        // replace heading funny stuff
        sanitized = sanitized.replaceAll("^[ ?:-]+", "_");

        // sanitize inside
        sanitized = sanitized.replaceAll("[/:\\|?$\\\\()#]", "_");

        // use OS specific Path class to test filename ... might work
        try {
            Path testPath = targetDirectory.resolve(sanitized);
        } catch (InvalidPathException e) {
            sanitized = sanitized.replaceAll("[^a-zA-Z0-9]", "_");
            log.debug("Sanitizing for filesystem {} -> {}", in, sanitized);
        }


        // limit file name length, Linux can process 256 char file names
        if (sanitized.length() > 192) {
            sanitized = sanitized.substring(0, 192);
        }

        return sanitized;
    }

    /** Get meta data field falling back to a default.
     * @param metaData meta data to get field from or {@code null}.
     * @param getter meta data field getter.
     * @param fallback fallback value of metaData or the field itself is {@code null}.
     * @return filesystem-sanitized metadata field content or fallback if field is not given.
     * */
    private String getMetaDataField(MetaData metaData, Function<MetaData, Optional<String>> getter, String fallback) {
        String result = fallback;
        if (metaData != null) {
            Optional<String> value = getter.apply(metaData);
            if (value != null && value.isPresent()) {
                result = value.get().trim();
            }
        }
        return sanitizeFileName(result);
    }

    /** Gets a string substitutor for file name generation. */
    private StringSubstitutor newStringSubstitutor(Radio radio, MetaData metaData, MimeType mimeTypeNullable) {
        String unknown = "unknown";
        Map<String, String> values = new HashMap<>();

        values.put("id", Integer.toString(consumerId));
        values.put("title", getMetaDataField(metaData, MetaData::getTitle, unknown));
        values.put("artist", getMetaDataField(metaData, MetaData::getArtist, unknown));

        values.put("stationUrl", getMetaDataField(metaData, MetaData::getStationUrl, unknown));
        values.put("stationName", getMetaDataField(metaData, MetaData::getStationName, unknown));
        values.put("stationHost", getMetaDataField(metaData,
                m -> m.getStationUrl().map(url -> URI.create(url).getHost()), unknown));

        // radio metadata
        values.put("radioName", sanitizeFileName(radio.getName().trim()));
        values.put("radioHost", sanitizeFileName(radio.getUri().getHost()));
        values.put("radioUri", sanitizeFileName(radio.getUri().toASCIIString()));

        // composite of station name with fallback radio uri
        values.put("stationNameOrRadioName",
                getMetaDataField(metaData,
                        MetaData::getStationName,
                        sanitizeFileName(radio.getName())));

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

        if ((metaData != null || !requireMetaData) && contentType != null) {
            StringSubstitutor substitutor = newStringSubstitutor(radio, metaData, contentType);
            String targetName = substitutor.replace(fileNameFormat);
            Path path = targetDirectory.resolve(targetName);
            if (Files.exists(path)) {
                path = generateNonExistingPath(path);
            }

            if (! path.startsWith(targetDirectory)) {
                throw new IllegalArgumentException("Generated file name "+path+" is outside target directory " + targetDirectory);
            }

            result = Optional.of(path);
        }
        return result;
    }

    /**
     * Generate a non-existing path from an existing one.
     */
    static Path generateNonExistingPath(Path currentPath) {
        Pattern withCountPattern = Pattern.compile("(.*)-([0-9]+)(\\.[^.]+)");
        Pattern withoutCountPattern = Pattern.compile("(.*)(\\.[^.]+)");
        while (Files.exists(currentPath)) {
            Path parent = currentPath.getParent();
            String fileName = currentPath.getFileName().toString();

            Matcher matcher = withCountPattern.matcher(fileName);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String count = matcher.group(2);
                String suffix = matcher.group(3);
                int countInt = Integer.parseInt(count);

                String newName = String.format("%s-%d%s", prefix, countInt + 1, suffix);
                currentPath = parent.resolve(newName);
            } else {
                matcher = withoutCountPattern.matcher(fileName);
                if (matcher.matches()) {
                    String prefix = matcher.group(1);
                    String suffix = matcher.group(2);

                    String newName = String.format("%s-%d%s", prefix, 1, suffix);
                    currentPath = parent.resolve(newName);
                }
            }
        }
        return currentPath;
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
