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
package fr.cnes.regards.modules.storage.rest;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.oais.urn.validator.RegardsOaisUrnAsString;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPPageWithDataStorages;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AIPWithDataStorageIds;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.DataFileDto;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.DataStorageRemovalAIPFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * REST controller handling request about {@link AIP}s
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
@RequestMapping(AIPController.AIP_PATH)
public class AIPController implements IResourceController<AIP> {

    public static final String FILES_DELETE_PATH = "/files/delete";

    /**
     * Controller path for retries
     */
    static final String RETRY_STORE_PATH = "/retry";

    /**
     * Controller base path
     */
    static final String AIP_PATH = "/aips";

    /**
     * Controller path for indexing
     */
    static final String INDEXING_PATH = "/indexing";

    /**
     * Controller path for bulk aip requests
     */
    static final String AIP_BULK = "/bulk";

    /**
     * Controller path for bulk aip requests deletion
     */
    static final String AIP_BULK_DELETE = "/bulkdelete";

    /**
     * Controller path to ask for dataFiles
     */
    static final String PREPARE_DATA_FILES = "/dataFiles";

    /**
     * AIP ID path parameter
     */
    static final String AIP_ID_PATH_PARAM = "aip_id";

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String AIP_ID_PATH = "/{" + AIP_ID_PATH_PARAM + "}";

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String AIP_ID_RETRY_STORE_PATH = AIP_ID_PATH + RETRY_STORE_PATH;

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String OBJECT_LINK_PATH = AIP_ID_PATH + "/objectlinks";

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String VERSION_PATH = AIP_ID_PATH + "/versions";

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String HISTORY_PATH = AIP_ID_PATH + "/history";

    /**
     * Controller path using an aip ip id as path variable
     */
    static final String TAG_PATH = AIP_ID_PATH + "/tags";

    /**
     * Controller path using an aip ip id and a tag as path variable
     */
    static final String TAG = TAG_PATH + "/{tag}";

    /**
     * Controller path using an aip ip id and a file checksum as path variable
     */
    static final String DOWNLOAD_AIP_FILE = "/{aip_id}/files/{checksum}";

    /**
     * Controller path to manage tags of multiple AIPs
     */
    static final String TAG_MANAGEMENT_PATH = "/tags";

    /**
     * Endpoint path to delete tags of multiple AIPs
     */
    static final String TAG_DELETION_PATH = TAG_MANAGEMENT_PATH + "/delete";

    /**
     * Controller path to search used tags by multiple AIPs
     */
    static final String TAG_SEARCH_PATH = TAG_MANAGEMENT_PATH + "/search";

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * {@link IAIPService} instance
     */
    @Autowired
    private IAIPService aipService;

    @Autowired
    private IProjectsClient projectsClient;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${zuul.prefix}")
    private String gatewayPrefix;

    @Value("${spring.application.name}")
    private String microserviceName;

    private static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }

    /**
     * Retrieve a page of aip metadata according to the given parameters
     * @param state state the aips should be in
     * @param from date(UTC) after which the aip should have been added to the system
     * @param to date(UTC) before which the aip should have been added to the system
     * @param tags
     * @param providerId
     * @param session {@link String}
     * @param pageable
     * @param assembler
     * @return page of aip metadata respecting the constraints
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips")
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState state,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to, @RequestParam(name = "tags", required = false) List<String> tags,
            @RequestParam(name = "providerId", required = false) String providerId,
            @RequestParam(name = "session", required = false) String session,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AIP> assembler) throws ModuleException {
        Page<AIP> aips = aipService.retrieveAIPs(state, from, to, tags, session, providerId, pageable);
        return new ResponseEntity<>(toPagedResources(aips, assembler), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips")
    public ResponseEntity<AIPPageWithDataStorages> retrieveAIPWithDataStorages(
            AIPQueryFilters filters,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AIPWithDataStorageIds> assembler) throws ModuleException {
        AIPPageWithDataStorages aipPage = aipService.retrieveAIPWithDataStorageIds(filters, pageable);
        return new ResponseEntity<>(aipPage, HttpStatus.OK);
    }

    /**
     * Retrieve a page of aip with indexing information on associated files according to the given parameters
     * @param state
     * @param inTags
     * @param fromLastUpdateDate
     * @param pageable
     * @return page of aip with indexing information on associated files respecting the constraints
     * @throws MalformedURLException
     */
    @RequestMapping(method = RequestMethod.GET, path = INDEXING_PATH)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips with indexing information on associated files")
    public ResponseEntity<PagedResources<AipDataFiles>> retrieveAipDataFiles(
            @RequestParam(name = "state") AIPState state,
            @RequestParam(value = "tags", required = false) Set<String> inTags,
            @RequestParam(name = "last_update", required = false) String fromLastUpdateDate,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable)
            throws MalformedURLException {
        OffsetDateTime fromLastUpdate = null;
        if (!Strings.isNullOrEmpty(fromLastUpdateDate)) {
            fromLastUpdate = OffsetDateTimeAdapter.parse(fromLastUpdateDate);
        }
        Set<String> tags = inTags == null ? Collections.emptySet() : inTags;
        Page<AipDataFiles> page = aipService.retrieveAIPDataFiles(state, tags, fromLastUpdate, pageable);
        List<AipDataFiles> content = page.getContent();
        // Now that we have our data files, lets compute their public URL
        // First lets get the public hostname from rs-admin-instance
        FeignSecurityManager.asSystem();
        String projectHost = projectsClient.retrieveProject(runtimeTenantResolver.getTenant()).getBody().getContent()
                .getHost();
        FeignSecurityManager.reset();

        for (AipDataFiles aipData : content) {
            for (DataFileDto dataFileDto : aipData.getDataFiles()) {
                toPublicDataFile(projectHost, dataFileDto, aipData.getAip());
            }
        }
        // small hack to be used thanks to GSON which does not know how to deserialize Page or PageImpl
        PagedResources<AipDataFiles> aipDataFiles = new PagedResources<>(content,
                                                                         new PagedResources.PageMetadata(page.getSize(),
                                                                                                         page.getNumber(),
                                                                                                         page.getTotalElements(),
                                                                                                         page.getTotalPages()));
        return new ResponseEntity<>(aipDataFiles, HttpStatus.OK);
    }

    /**
     * Same as {@link AIPController#storeRetryUnit(String)} with multiple aips
     * @param aipIpIds
     * @return {@link RejectedAip}s
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, value = RETRY_STORE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Retry to store given aips, threw their ip id")
    public ResponseEntity<List<RejectedAip>> storeRetry(@RequestBody @Valid Set<String> aipIpIds)
            throws ModuleException {
        List<RejectedAip> rejectedAips = aipService.applyRetryChecks(aipIpIds);
        if (!rejectedAips.isEmpty()) {
            rejectedAips.forEach(ra -> aipIpIds.remove(ra.getAipId()));
            if (aipIpIds.isEmpty()) {
                return new ResponseEntity<>(rejectedAips, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            aipService.storeRetry(aipIpIds);
            return new ResponseEntity<>(rejectedAips, HttpStatus.PARTIAL_CONTENT);
        }
        aipService.storeRetry(aipIpIds);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retry the storage of an aip
     * @param ipId
     * @return whether the aip could be scheduled for storage or not
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, value = AIP_ID_RETRY_STORE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Retry to store given aip, threw its ip id")
    public ResponseEntity<RejectedAip> storeRetryUnit(@PathVariable(AIP_ID_PATH_PARAM) String ipId)
            throws ModuleException {
        // we ask for one AIP to be stored, so we can only have one rejected aip in counter part
        ResponseEntity<List<RejectedAip>> listResponse = storeRetry(Sets.newHashSet(ipId));
        // as their is only one ip id, storeRetry can only give us a UNPROCESSABLE_ENTITY or NO_CONTENT
        if (listResponse.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
            return new ResponseEntity<>(listResponse.getBody().get(0), listResponse.getStatusCode());
        } else {
            return new ResponseEntity<>(listResponse.getStatusCode());
        }
    }

    /**
     * Ask for the storage of the aips into the collection
     * @param aips
     * @return the aips that could not be prepared for storage
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    @ResourceAccess(description = "validate and store the specified AIPs")
    // Do not user @Valid on this endpoint as aip validation is done by hand
    public ResponseEntity<List<RejectedAip>> store(@RequestBody AIPCollection aips) throws ModuleException {
        int originalAipNb = aips.getFeatures().size();

        // Just store AIP / Heavy work will be done asynchronously
        List<RejectedAip> rejectedAips = aipService.validateAndStore(aips);

        // Manage API response
        if (rejectedAips.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else if (rejectedAips.size() == originalAipNb) {
            return new ResponseEntity<>(rejectedAips, HttpStatus.UNPROCESSABLE_ENTITY);
        } else {
            return new ResponseEntity<>(rejectedAips, HttpStatus.PARTIAL_CONTENT);
        }
    }

    /**
     * Delete aips that are associated to at least one of the provided sips, represented by their ip id
     * @param sipIds
     * @return SIP for which the deletion could not be made
     * @throws ModuleException
     */
    @RequestMapping(value = AIP_BULK_DELETE, method = RequestMethod.POST)
    @ResourceAccess(description = "Delete AIPs associated to the given SIP", role = DefaultRole.ADMIN)
    public ResponseEntity<List<RejectedSip>> deleteAipFromSips(@RequestBody Set<String> sipIds) throws ModuleException {
        List<RejectedSip> notHandledSips;
        long start = System.currentTimeMillis();
        notHandledSips = aipService.deleteAipFromSips(sipIds);
        LOGGER.trace("Deleting {} sips took {} ms", sipIds.size(), System.currentTimeMillis() - start);
        if (notHandledSips.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            if (notHandledSips.size() == sipIds.size()) {
                return new ResponseEntity<>(notHandledSips, HttpStatus.UNPROCESSABLE_ENTITY);
            } else {
                return new ResponseEntity<>(notHandledSips, HttpStatus.PARTIAL_CONTENT);
            }
        }
    }

    /**
     * Retrieve the aip files metadata associated to an aip, represented by its ip id
     * @param ipId
     * @return aip files metadata associated to the aip
     * @throws ModuleException
     */
    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files metadata of a specified aip")
    public ResponseEntity<Set<OAISDataObject>> retrieveAIPFiles(@PathVariable(AIP_ID_PATH_PARAM) @Valid String ipId)
            throws ModuleException {
        Set<OAISDataObject> files = aipService.retrieveAIPFiles(UniformResourceName.fromString(ipId));
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    /**
     * Ask for the files into the availability request to be set into the cache
     * @param availabilityRequest
     * @return the files that are already available and those that could not be set into the cache
     * @throws ModuleException
     */
    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "allow to request download availability for given files and return already "
            + "available files", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<AvailabilityResponse> makeFilesAvailable(
            @Valid @RequestBody AvailabilityRequest availabilityRequest) throws ModuleException {
        return ResponseEntity.ok(aipService.loadFiles(availabilityRequest));
    }

    /**
     * Retrieve all aips which ip id is one of the provided ones
     * @param ipIds
     * @return aips as an {@link AIPCollection}
     */
    @RequestMapping(value = AIP_BULK, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to retrieve a collection of aip corresponding to the given set of ids")
    @ResponseBody
    public ResponseEntity<AIPCollection> retrieveAipsBulk(
            @RequestBody @Valid @RegardsOaisUrnAsString Set<String> ipIds) {
        Set<AIP> aips = aipService.retrieveAipsBulk(ipIds);
        AIPCollection aipCollection = new AIPCollection();
        aipCollection.addAll(aips);
        // if we have everything, then we return HttpStatus OK(200)
        if (aips.stream().map(aip -> aip.getId().toString()).collect(Collectors.toSet()).containsAll(ipIds)) {
            return new ResponseEntity<>(aipCollection, HttpStatus.OK);
        } else {
            // Otherwise, HttpStatus PARTIAL_CONTENT(206)
            return new ResponseEntity<>(aipCollection, HttpStatus.PARTIAL_CONTENT);
        }
    }

    /**
     * Retrieve the aip for the given aipId
     * @param aipId
     * @return the aip
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = AIP_ID_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "allow to retrieve a given aip metadata thanks to its ipId")
    @ResponseBody
    public ResponseEntity<AIP> retrieveAip(@PathVariable(name = AIP_ID_PATH_PARAM) String aipId)
            throws EntityNotFoundException {
        return new ResponseEntity<>(aipService.retrieveAip(aipId), HttpStatus.OK);
    }

    /**
     * Add tags to an aip, represented by its ip id
     * @param ipId
     * @param toAddTags
     * @return Void
     * @throws EntityException
     */
    @RequestMapping(value = TAG_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to add multiple tags to a given aip")
    @ResponseBody
    public ResponseEntity<Void> addTags(@PathVariable(name = AIP_ID_PATH_PARAM) String ipId,
            @RequestBody Set<String> toAddTags) throws EntityException {
        aipService.addTags(ipId, toAddTags);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Add tags to a list of aips
     * @param request
     * @return Void
     */
    @RequestMapping(value = TAG_MANAGEMENT_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to add multiple tags to several aips")
    @ResponseBody
    public ResponseEntity<Void> addTagsByQuery(@Valid @RequestBody AddAIPTagsFilters request) {
        boolean succeed = aipService.addTagsByQuery(request);
        if (succeed) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    /**
     * Remove tags from a given aip, represented by its ip id
     * @param ipId
     * @param toRemoveTags
     * @return Void
     * @throws EntityException
     */
    @RequestMapping(value = TAG_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allow to remove multiple tags to a given aip")
    @ResponseBody
    public ResponseEntity<Void> removeTags(@PathVariable(name = AIP_ID_PATH_PARAM) String ipId,
            @RequestBody Set<String> toRemoveTags) throws EntityException {
        aipService.removeTags(ipId, toRemoveTags);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Remove tags from a list of aips
     * @param request
     * @return Void
     */
    @RequestMapping(value = TAG_DELETION_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to remove multiple tags to several aips")
    @ResponseBody
    public ResponseEntity<Void> removeTagsByQuery(@Valid @RequestBody RemoveAIPTagsFilters request) {
        boolean succeed = aipService.removeTagsByQuery(request);
        if (succeed) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    /**
     * Retrieve common tags from a list of aips
     * @param request
     * @return tags
     */
    @RequestMapping(value = TAG_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to search tags used by aips")
    public ResponseEntity<List<String>> retrieveAIPTags(@Valid @RequestBody AIPQueryFilters request) {
        List<String> aipTags = aipService.retrieveAIPTagsByQuery(request);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    @RequestMapping(value = FILES_DELETE_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to remove aips files from given data storages")
    public ResponseEntity<Void> deleteAIPFilesOnDataStorage(@Valid @RequestBody DataStorageRemovalAIPFilters request) {
        for (Long dataStorageId : request.getDataStorageIds()) {
            aipService.deleteFilesFromDataStorageByQuery(request, dataStorageId);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieve all aips which are tagged by the provided tag
     * @param tag
     * @param pageable
     * @param assembler
     * @return aips tagged by the tag
     */
    @RequestMapping(value = TAG, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve a collection of AIP according to a tag")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAipsByTag(@PathVariable("tag") String tag,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            final PagedResourcesAssembler<AIP> assembler) {
        return ResponseEntity.ok(toPagedResources(aipService.retrieveAipsByTag(tag, pageable), assembler));
    }

    /**
     * Update an aip, represented by its ip id, thanks to the provided pojo
     * @param ipId
     * @param updated
     * @return updated aip
     * @throws EntityException
     */
    @RequestMapping(value = AIP_ID_PATH, method = RequestMethod.PUT,
            consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    @ResourceAccess(description = "allow to update a given aip metadata")
    @ResponseBody
    public ResponseEntity<Void> updateAip(@PathVariable(name = AIP_ID_PATH_PARAM) String ipId,
            @RequestBody @Valid AIP updated) throws EntityException {
        aipService.updateAip(ipId, updated, "Update AIP");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Delete an aip, represented by its ip id
     * @param ipId
     * @return Not deletable aips ids.
     * @throws ModuleException
     */
    @RequestMapping(value = AIP_ID_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allow to delete a given aip", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<String> deleteAip(@PathVariable(name = AIP_ID_PATH_PARAM) String ipId)
            throws ModuleException {
        Set<StorageDataFile> notSuppressible = aipService.deleteAip(ipId);
        if (notSuppressible.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            StringJoiner sj = new StringJoiner(", ",
                                               "This aip could not be deleted because at least one of his files has not be handle by the storage process: ",
                                               ".");
            notSuppressible.stream().map(StorageDataFile::getAipEntity)
                    .forEach(aipEntity -> sj.add(aipEntity.getAipId()));
            return new ResponseEntity<>(sj.toString(), HttpStatus.CONFLICT);
        }
    }

    /**
     * Retrieve the history of events that occurred on a given aip, represented by its ip id
     * @param ipId
     * @return the history of events that occurred to the aip
     * @throws ModuleException
     */
    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "send the history of event occurred on each data file of the specified AIP")
    @ResponseBody
    public ResponseEntity<List<Event>> retrieveAIPHistory(
            @PathVariable(AIP_ID_PATH_PARAM) @Valid UniformResourceName ipId) throws ModuleException {
        List<Event> history = aipService.retrieveAIPHistory(ipId);
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @RequestMapping(value = VERSION_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of versions of an aip through their ip ids")
    public ResponseEntity<List<String>> retrieveAIPVersionHistory(
            @PathVariable(AIP_ID_PATH_PARAM) @Valid UniformResourceName ipId) {
        List<String> versions = aipService.retrieveAIPVersionHistory(ipId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable(AIP_ID_PATH_PARAM) String aipId,
            @PathVariable("checksum") String checksum) throws ModuleException, IOException {
        // Retrieve file locale path, 404 if aip not found or bad checksum or no storage plugin
        // 403 if user has not right
        try {
            Pair<StorageDataFile, InputStream> dataFileISPair = aipService.getAIPDataFile(aipId, checksum);
            if (dataFileISPair != null) {
                StorageDataFile dataFile = dataFileISPair.getFirst();
                InputStreamResource isr = new InputStreamResource(dataFileISPair.getSecond());
                Long fileSize = dataFile.getFileSize();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentLength(fileSize);
                headers.setContentType(asMediaType(dataFile.getMimeType()));
                headers.setContentDispositionFormData("attachement;filename=", dataFile.getName());
                return new ResponseEntity<>(isr, headers, HttpStatus.OK);
            } else { // NEARLINE file not in cache
                return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
            }
        } catch (EntityOperationForbiddenException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public Resource<AIP> toResource(AIP element, Object... extras) {
        Resource<AIP> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveAIPs",
                                LinkRels.LIST,
                                MethodParamFactory.build(AIPState.class),
                                MethodParamFactory.build(OffsetDateTime.class),
                                MethodParamFactory.build(OffsetDateTime.class),
                                MethodParamFactory.build(List.class),
                                MethodParamFactory.build(String.class),
                                MethodParamFactory.build(String.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveAip",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getId().toString()));
        if (AIPState.STORAGE_ERROR.equals(element.getState())) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "storeRetryUnit",
                                    "retry",
                                    MethodParamFactory.build(String.class, element.getId().toString()));
        }
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteAip",
                                "delete",
                                MethodParamFactory.build(String.class, element.getId().toString()));
        return resource;
    }

    /**
     * Handles any changes that should occurred between private rs-storage information and how the rest of the world
     * should see them.
     * For example, change URL from file:// to http://[project_host]/[gateway prefix]/rs-storage/...
     */
    private void toPublicDataFile(String projectHost, DataFileDto dataFile, AIP owningAip)
            throws MalformedURLException {
        // Lets reconstruct the public url of rs-storage
        // now lets add it the gateway prefix and the microservice name and the endpoint path to it
        String sb = projectHost + "/" + gatewayPrefix + "/" + microserviceName + AIP_PATH + DOWNLOAD_AIP_FILE
                .replaceAll("\\{" + AIP_ID_PATH_PARAM + "\\}", owningAip.getId().toString())
                .replaceAll("\\{checksum\\}", dataFile.getChecksum());
        URL downloadUrl = new URL(sb);
        dataFile.setUrl(downloadUrl);
    }

}
