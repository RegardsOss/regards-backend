package fr.cnes.regards.modules.processing.domain;

import lombok.Value;
import lombok.With;

import java.net.URL;

@Value @With
public class POutputFile {

    /** The file name */
    String name;

    /** The file checksum */
    String checksum;

    /** Where to download from */
    URL url;

    /** The file size */
    Long size;

}
