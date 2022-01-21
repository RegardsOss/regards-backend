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

package fr.cnes.regards.modules.feature.service.dump;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.feature.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.feature.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;

/**
 * Service to dump features, which consists in creating zip of zip from feature formatted as aips
 * @author Iliana Ghazali
 */

public interface IFeatureMetadataService {

    /** Write temporary zips in the job workspace. These zips are composed of feature (in aip format) written in json files
     * @param metadataRequest request that contains the information about the dump
     * @param tmpZipLocation temporary location used to write zips
     * @throws NothingToDoException exception occurs if there is no aip to dump
     */
    void writeZips(FeatureSaveMetadataRequest metadataRequest, Path tmpZipLocation)
            throws NothingToDoException, IOException;

    /**
     * Util to write zips
     * @return next pageable if exist null otherwise
     * @throws RsRuntimeException when there is an issue while trying to dump this page(for example, duplicate names or IOException)
     */
    Pageable dumpOnePage(FeatureSaveMetadataRequest metadataRequest, Pageable pageToRequest, Path tmpZipLocation)
            throws IOException, DuplicateUniqueNameException, NothingToDoException;

    /** Dump feature contents (in aip format) by zipping all zips previously generated from feature contents
     * @param metadataRequest request that contains the information about the dump
     * @param dumpLocation location to write a zip made up of zips
     * @param tmpZipLocation temporary location to retrieve zips (in the job workspace)
     */
    void writeDump(FeatureSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation)
            throws IOException;

    /** Handle request in error and notify client
     * @param metadataRequest request that contains the information about the dump
     * @param errorMessage message indicating the reason the dump was not performed
     */
    void handleError(FeatureSaveMetadataRequest metadataRequest, String errorMessage);

    /** Handle request in success
     * @param metadataRequest request that contains the information about the dump
     */
    void handleSuccess(FeatureSaveMetadataRequest metadataRequest);

    /**
     * Find all {@link FeatureSaveMetadataRequest}s
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page
     * @return {@link FeatureSaveMetadataRequest}s
     */
    public Page<FeatureSaveMetadataRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);

    /**
     * Delete requests associated to given search parameters
     * Number of requests deletable is limited as this method is synchonous. Number of handled requests is returned in response.
     *
     * @param selection {@link FeatureRequestsSelectionDTO}
     */
    RequestHandledResponse deleteRequests(FeatureRequestsSelectionDTO selection);

    /**
     * Find requests information
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(FeatureRequestsSelectionDTO selection);

    /**
     * Retry requests associated to given search parameters
     * Number of requests deletable is limited as this method is synchonous. Number of handled requests is returned in response.
     *
     * @param selection {@link FeatureRequestsSelectionDTO}
     */
    RequestHandledResponse retryRequests(FeatureRequestsSelectionDTO selection);

}
