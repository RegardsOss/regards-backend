/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.client;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.RejectedSip;

/**
 * Feign client to interact with {@link AIP}s
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@RestClient(name = "rs-storage")
@RequestMapping(value = IAipClient.AIP_PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAipClient {

    /**
     * Client base path
     */
    String AIP_PATH = "/aips";

    /**
     * Controller path for bulk aip requests deletion
     */
    public static final String AIP_BULK_DELETE = "/bulkdelete";

    /**
     * Client path for indexing
     */
    String INDEXING_PATH = "/indexing";

    /**
     * Client path to retry the storage of multiple aips
     */
    String RETRY_STORE_PATH = "/retry";

    /**
     * Client path to put in cache files
     */
    String PREPARE_DATA_FILES = "/dataFiles";

    /**
     * Client path using an aip ip id as path variable
     */
    String ID_PATH = "/{ip_id}";

    /**
     * Client path used to retry the storage of an aip
     */
    String IP_ID_RETRY_STORE_PATH = ID_PATH + RETRY_STORE_PATH;

    /**
     * Client path used to download files
     */
    String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    /**
     * Client path used to get the history of the versions of an aip
     */
    String HISTORY_PATH = ID_PATH + "/history";

    /**
     * Client path using aip ip id and file checksum as path variable
     */
    String DOWLOAD_AIP_FILE = "/{ip_id}/files/{checksum}";

    /**
     * Retrieve a page of aip metadata according to the given parameters
     * @param pState state the aips should be in
     * @param pFrom date after which the aip should have been added to the system
     * @param pTo date before which the aip should have been added to the system
     * @param pPage page number
     * @param pSize page size
     * @return page of aip metadata respecting the constrains
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Retrieve a page of aip with indexing information on associated files according to the given parameters
     * @return page of aip with indexing information on associated files respecting the constraints
     */
    @RequestMapping(method = RequestMethod.GET, path = INDEXING_PATH)
    ResponseEntity<PagedResources<AipDataFiles>> retrieveAipDataFiles(@RequestParam(name = "state") AIPState state,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            @RequestParam(name = "last_update", required = false) OffsetDateTime fromLastUpdateDate,
            @RequestParam("page") int page, @RequestParam("size") int size);

    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    ResponseEntity<List<RejectedAip>> store(@RequestBody @Valid AIPCollection aips);

    /**
     * Same than {@link IAipClient#storeRetryUnit(String)} but for a bunch of aips
     */
    @RequestMapping(method = RequestMethod.POST, value = RETRY_STORE_PATH)
    ResponseEntity<List<RejectedAip>> storeRetry(@RequestBody @Valid Set<String> aipIpIds);

    /**
     * Allows to ask for the storage of an aip which has failed
     * @return whether the aip could be scheduled for storage
     */
    @RequestMapping(method = RequestMethod.POST, value = IP_ID_RETRY_STORE_PATH)
    ResponseEntity<RejectedAip> storeRetryUnit(@PathVariable("ip_id") String ipId);

    /**
     * Delete aips that are associated to the given SIP, represented by their ip id
     * @return list of sip for which we could not delete the linked aips
     */
    @RequestMapping(value = AIP_BULK_DELETE, method = RequestMethod.POST)
    public ResponseEntity<List<RejectedSip>> deleteAipFromSips(@RequestBody Set<String> sipIpIds);

    /**
     * Retrieve the meta data of files associated to an aip, represented by its ip id
     * @return list of files meta data associated to an aip
     */
    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    ResponseEntity<List<OAISDataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId);

    /**
     * Retrieve a page of the different versions of an aip, represented by its ip id
     * @param pPage page number
     * @param pSize page size
     * @return list of ip ids of the different versions of an aip
     */
    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    ResponseEntity<List<String>> retrieveAIPVersionHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId,
            @RequestParam("page") int pPage, @RequestParam("size") int pSize);

    /**
     * Request that files are made available for downloading
     * @return the list of file already available via their checksums
     */
    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    ResponseEntity<AvailabilityResponse> makeFilesAvailable(@RequestBody AvailabilityRequest availabilityRequest);

    @RequestMapping(path = DOWLOAD_AIP_FILE, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response downloadFile(@PathVariable("ip_id") String aipId, @PathVariable("checksum") String checksum);
    
    /**
     * Update an aip, represented by its ip id, thanks to the provided pojo
     * @return updated aip
     */
    @RequestMapping(value = ID_PATH, method = RequestMethod.PUT,
            consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    public ResponseEntity<AIP> updateAip(@PathVariable(name = "ip_id") String ipId, @RequestBody @Valid AIP updated);
}
