/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This class defines an execution output file.
 * <p>
 * An output file is immutable.
 *
 * @author gandrieu
 */
@Value
public class POutputFile {

    @Value
    public static class Digest {

        String method;

        String value;
    }

    /**
     * The output file id
     */
    UUID id;

    /**
     * The execution this file has been generated for
     */
    UUID execId;

    /**
     * The file name, or relative path, to the execution workdir
     */
    String name;

    /**
     * The file checksum
     */
    Digest checksum;

    /**
     * Where to download from
     */
    URL url;

    /**
     * The file size
     */
    Long size;

    /**
     * The list of input correlation IDs this output file is related to.
     */
    List<String> inputCorrelationIds;

    /**
     * Date at which the file was created
     */
    transient OffsetDateTime created;

    /**
     * Whether the file has been downloaded or not
     */
    transient boolean downloaded;

    /**
     * Whether the file has been deleted or not
     */
    transient boolean deleted;

    /**
     * This information leaks from the database but needs to be kept in the domain.
     * It allows the database layer to know if it must CREATE or UPDATE the database
     * for this instance.
     */
    transient boolean persisted;

    public static POutputFile markDownloaded(POutputFile pOutputFile) {
        return pOutputFile.withDownloaded(true);
    }

    public POutputFile withId(UUID id) {
        return this.id.equals(id) ?
            this :
            new POutputFile(id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withExecId(UUID execId) {
        return this.execId.equals(execId) ?
            this :
            new POutputFile(this.id,
                            execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withName(String name) {
        return this.name.equals(name) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withChecksum(Digest checksum) {
        return this.checksum.equals(checksum) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withUrl(URL url) {
        return this.url.equals(url) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withSize(Long size) {
        return this.size.equals(size) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withInputCorrelationIds(List<String> inputCorrelationIds) {
        return this.inputCorrelationIds.equals(inputCorrelationIds) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withCreated(OffsetDateTime created) {
        return this.created.equals(created) ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            created,
                            this.downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withDownloaded(boolean downloaded) {
        return this.downloaded == downloaded ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            downloaded,
                            this.deleted,
                            this.persisted);
    }

    public POutputFile withDeleted(boolean deleted) {
        return this.deleted == deleted ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            deleted,
                            this.persisted);
    }

    public POutputFile withPersisted(boolean persisted) {
        return this.persisted == persisted ?
            this :
            new POutputFile(this.id,
                            this.execId,
                            this.name,
                            this.checksum,
                            this.url,
                            this.size,
                            this.inputCorrelationIds,
                            this.created,
                            this.downloaded,
                            this.deleted,
                            persisted);
    }
}
