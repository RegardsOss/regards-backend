/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;

/**
 * Manage AIP dumps
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAIPMetadataService {

    /** Write zip in workspace  */
    void writeZips(AIPSaveMetadataRequest metadataRequest, Path tmpZipLocation)
            throws NothingToDoException;

    /** Create zip of zips (in workspace by default) */
    void writeDump(AIPSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation);

    /**
     * Get set of aips to zip and zip their content in workspace
     * @return next pageable if exist null otherwise
     * @throws RsRuntimeException when there is an issue while trying to dump this page(for example, duplicate names or IOException)
     */
    Pageable dumpOnePage(AIPSaveMetadataRequest metadataRequest, Pageable pageToRequest,
            Path workspace) throws IOException, DuplicateUniqueNameException, NothingToDoException;

    /** Reset last dump date */
    void resetLastUpdateDate();

    /** Handle request in error and notify client */
    void handleError(AIPSaveMetadataRequest metadataRequest, String errorMessage);

    /** Handle request in success */
    void handleSuccess(AIPSaveMetadataRequest metadataRequest);
}
