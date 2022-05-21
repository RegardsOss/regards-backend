/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.dump;

import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Service to dump aips, which consists in creating a zip of zips from aip contents.
 *
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAIPMetadataService {

    /**
     * Write temporary zips composed of aip json files in the job workspace
     *
     * @param metadataRequest request that contains the information about the dump
     * @param tmpZipLocation  temporary location used to write zips
     * @throws NothingToDoException exception occurs if there is no aip to dump
     */
    void writeZips(AIPSaveMetadataRequest metadataRequest, Path tmpZipLocation)
        throws NothingToDoException, IOException;

    /**
     * Util to write zips
     *
     * @return next pageable if exist null otherwise
     * @throws RsRuntimeException when there is an issue while trying to dump this page(for example, duplicate names or IOException)
     */
    Pageable dumpOnePage(AIPSaveMetadataRequest metadataRequest, Pageable pageToRequest, Path tmpZipLocation)
        throws IOException, DuplicateUniqueNameException, NothingToDoException;

    /**
     * Dump aip contents by zipping all zips previously generated from aip contents
     *
     * @param metadataRequest request that contains the information about the dump
     * @param dumpLocation    location to write a zip made up of zips
     * @param tmpZipLocation  temporary location to retrieve zips (in the job workspace)
     */
    void writeDump(AIPSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation) throws IOException;

    /**
     * Handle request in error and notify client
     *
     * @param metadataRequest request that contains the information about the dump
     * @param errorMessage    message indicating the reason the dump was not performed
     */
    void handleError(AIPSaveMetadataRequest metadataRequest, String errorMessage);

    /**
     * Handle request in success
     *
     * @param metadataRequest request that contains the information about the dump
     */
    void handleSuccess(AIPSaveMetadataRequest metadataRequest);
}
