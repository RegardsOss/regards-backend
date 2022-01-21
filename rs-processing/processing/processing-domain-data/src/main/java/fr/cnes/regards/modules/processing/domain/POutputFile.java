/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.domain;

import io.vavr.collection.List;
import lombok.Value;
import lombok.With;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This class defines an execution output file.
 *
 * An output file is immutable.
 *
 * @author gandrieu
 */
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

    /** The file name, or relative path, to the execution workdir */
    String name;

    /** The file checksum */
    Digest checksum;

    /** Where to download from */
    URL url;

    /** The file size */
    Long size;

    /** The list of input correlation IDs this output file is related to. */
    List<String> inputCorrelationIds;

    /** Date at which the file was created */
    transient OffsetDateTime created;

    /** Whether the file has been downloaded or not */
    transient boolean downloaded;

    /** Whether the file has been deleted or not */
    transient boolean deleted;

    /** This information leaks from the database but needs to be kept in the domain.
     * It allows the database layer to know if it must CREATE or UPDATE the database
     * for this instance. */
    transient boolean persisted;

    public static POutputFile markDownloaded(POutputFile pOutputFile) {
        return pOutputFile.withDownloaded(true);
    }
}
