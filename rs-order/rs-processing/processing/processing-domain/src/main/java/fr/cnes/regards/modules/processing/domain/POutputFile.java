package fr.cnes.regards.modules.processing.domain;

import lombok.Value;
import lombok.With;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value @With
public class POutputFile {

    @Value
    public static class Digest {
        String method;
        String value;
    }

    /** The output file id */
    UUID id;

    /** The execution this file has been generated for */
    UUID execId;

    /** The file name */
    String name;

    /** The file checksum */
    Digest checksum;

    /** Where to download from */
    URL url;

    /** The file size */
    Long size;

    /** Date at which the file was created */
    transient OffsetDateTime created;

    /** Whether the file has been downloaded or not */
    transient boolean downloaded;

    /** Whether the file has been deleted or not */
    transient boolean deleted;

}
