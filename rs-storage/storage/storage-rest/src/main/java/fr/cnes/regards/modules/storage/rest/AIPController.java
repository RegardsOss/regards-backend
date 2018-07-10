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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
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
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.DataFileDto;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.service.IAIPService;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
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

/**
 * REST controller handling request about {@link AIP}s
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
@RequestMapping(AIPController.AIP_PATH)
public class AIPController implements IResourceController<AIP> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    /**
     * Controller path for retries
     */
    public static final String RETRY_STORE_PATH = "/retry";

    /**
     * Controller base path
     */
    public static final String AIP_PATH = "/aips";

    /**
     * Controller path for indexing
     */
    public static final String INDEXING_PATH = "/indexing";

    /**
     * Controller path for bulk aip requests
     */
    public static final String AIP_BULK = "/bulk";

    /**
     * Controller path for bulk aip requests deletion
     */
    public static final String AIP_BULK_DELETE = "/bulkdelete";

    /**
     * Controller path to ask for dataFiles
     */
    public static final String PREPARE_DATA_FILES = "/dataFiles";

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String ID_PATH = "/{ip_id}";

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String IP_ID_RETRY_STORE_PATH = ID_PATH + RETRY_STORE_PATH;

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String VERSION_PATH = ID_PATH + "/versions";

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String HISTORY_PATH = ID_PATH + "/history";

    /**
     * Controller path using an aip ip id as path variable
     */
    public static final String TAG_PATH = ID_PATH + "/tags";

    /**
     * Controller path using an aip ip id and a tag as path variable
     */
    public static final String TAG = TAG_PATH + "/{tag}";

    /**
     * Controller path using an aip ip id and a file checksum as path variable
     */
    public static final String DOWNLOAD_AIP_FILE = "/{ip_id}/files/{checksum}";

    /**
     * Controller path to manage tags of multiple AIPs
     */
    public static final String TAG_MANAGEMENT_PATH = "/tags";

    /**
     * Endpoint path to delete tags of multiple AIPs
     */
    public static final String TAG_DELETION_PATH = "/delete";

    /**
     * Controller path to search used tags by multiple AIPs
     */
    public static final String TAG_SEARCH_PATH = TAG_MANAGEMENT_PATH + "/search";

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

    public static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }

    /**
     * Retrieve a page of aip metadata according to the given parameters
     * @param pState  state the aips should be in
     * @param from    date(UTC) after which the aip should have been added to the system
     * @param to      date(UTC) before which the aip should have been added to the system
     * @param session {@link String}
     * @return page of aip metadata respecting the constraints
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips")
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "tags", required = false) List<String> tags,
            @RequestParam(name = "session", required = false) String session,
            final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws ModuleException {
        Page<AIP> aips = aipService.retrieveAIPs(pState, from, to, tags, session, pPageable);
        return new ResponseEntity<>(toPagedResources(aips, pAssembler), HttpStatus.OK);
    }

    /**
     * Retrieve a page of aip with indexing information on associated files according to the given parameters
     * @return page of aip with indexing information on associated files respecting the constraints
     */
    @RequestMapping(method = RequestMethod.GET, path = INDEXING_PATH)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips with indexing information on associated files")
    public ResponseEntity<PagedResources<AipDataFiles>> retrieveAipDataFiles(
            @RequestParam(name = "state") AIPState state,
            @RequestParam(value = "tags", required = false) Set<String> inTags,
            @RequestParam(name = "last_update", required = false) String fromLastUpdateDate, final Pageable pageable)
            throws MalformedURLException {
        OffsetDateTime fromLastUpdate = null;
        if (!Strings.isNullOrEmpty(fromLastUpdateDate)) {
            fromLastUpdate = OffsetDateTimeAdapter.parse(fromLastUpdateDate);
        }
        Set<String> tags = (inTags == null) ? Collections.emptySet() : inTags;
        Page<AipDataFiles> page = aipService.retrieveAipDataFiles(state, tags, fromLastUpdate, pageable);
        List<AipDataFiles> content = page.getContent();
        for (AipDataFiles aipData : content) {
            for (DataFileDto dataFileDto : aipData.getDataFiles()) {
                toPublicDataFile(dataFileDto, aipData.getAip());
            }
        }
        // small hack to be used thanks to GSON which does not know how to deserialize Page or PageImpl
        PagedResources<AipDataFiles> aipDataFiles = new PagedResources<>(content, new PagedResources.PageMetadata(
                page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));
        return new ResponseEntity<>(aipDataFiles, HttpStatus.OK);
    }

    /**
     * Same as {@link AIPController#storeRetryUnit(String)} with multiple aips
     */
    @RequestMapping(method = RequestMethod.POST, value = RETRY_STORE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Retry to store given aips, threw their ip id")
    public ResponseEntity<List<RejectedAip>> storeRetry(@RequestBody @Valid Set<String> aipIpIds)
            throws ModuleException {
        List<RejectedAip> rejectedAips = aipService.applyRetryChecks(aipIpIds);
        if (!rejectedAips.isEmpty()) {
            rejectedAips.forEach(ra -> aipIpIds.remove(ra.getIpId()));
            if (aipIpIds.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
            }
            aipService.storeRetry(aipIpIds);
            return new ResponseEntity<>(rejectedAips, HttpStatus.PARTIAL_CONTENT);
        }
        aipService.storeRetry(aipIpIds);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retry the storage of an aip
     * @return whether the aip could be scheduled for storage or not
     */
    @RequestMapping(method = RequestMethod.POST, value = IP_ID_RETRY_STORE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Retry to store given aip, threw its ip id")
    public ResponseEntity<RejectedAip> storeRetryUnit(@PathVariable("ip_id") String ipId) throws ModuleException {
        // we ask for one AIP to be stored, so we can only have one rejected aip in counter part
        ResponseEntity<List<RejectedAip>> listResponse = storeRetry(Sets.newHashSet(ipId));
        if (listResponse.getBody().isEmpty()) {
            return new ResponseEntity<>(listResponse.getStatusCode());
        } else {
            return new ResponseEntity<>(listResponse.getBody().get(0), listResponse.getStatusCode());
        }
    }

    /**
     * Ask for the storage of the aips into the collection
     * @return the aips that could not be prepared for storage
     */
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    @ResourceAccess(description = "validate and store the specified AIPs")
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
     * @return SIP for which the deletion could not be made
     */
    @RequestMapping(value = AIP_BULK_DELETE, method = RequestMethod.POST)
    @ResourceAccess(description = "Delete AIPs associated to the given SIP", role = DefaultRole.ADMIN)
    public ResponseEntity<List<RejectedSip>> deleteAipFromSips(@RequestBody Set<String> sipIpIds)
            throws ModuleException {
        List<RejectedSip> notHandledSips = Lists.newArrayList();
        for (String sipIpId : sipIpIds) {
            Set<StorageDataFile> notSuppressible = aipService.deleteAipFromSip(sipIpId);
            if (!notSuppressible.isEmpty()) {
                StringJoiner sj = new StringJoiner(", ",
                        "This sip could not be deleted because at least one of its aip file has not be handle by the storage process: ",
                        ".");
                notSuppressible.stream().map(sdf -> sdf.getAipEntity())
                        .forEach(aipEntity -> sj.add(aipEntity.getIpId()));
                notHandledSips.add(new RejectedSip(sipIpId, sj.toString()));
            }
        }
        if (notHandledSips.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            if (notHandledSips.size() == sipIpIds.size()) {
                return new ResponseEntity<>(notHandledSips, HttpStatus.UNPROCESSABLE_ENTITY);
            } else {
                return new ResponseEntity<>(notHandledSips, HttpStatus.PARTIAL_CONTENT);
            }
        }
    }

    /**
     * Retrieve the aip files metadata associated to an aip, represented by its ip id
     * @return aip files metadata associated to the aip
     */
    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files metadata of a specified aip")
    public ResponseEntity<Set<Resource<OAISDataObject>>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId)
            throws ModuleException {
        Set<OAISDataObject> files = aipService.retrieveAIPFiles(UniformResourceName.fromString(pIpId));
        // Adapt the result to match front expectations
        Set<Resource<OAISDataObject>> result = new HashSet<>(files.size());
        files.forEach(f -> result.add(new Resource<>(f)));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Ask for the files into the availability request to be set into the cache
     * @return the files that are already available and those that could not be set into the cache
     */
    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "allow to request download availability for given files and return already "
            + "available files", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<AvailabilityResponse> makeFilesAvailable(@RequestBody AvailabilityRequest availabilityRequest)
            throws ModuleException {
        return ResponseEntity.ok(aipService.loadFiles(availabilityRequest));
    }

    /**
     * Retrieve all aips which ip id is one of the provided ones
     * @return aips as an {@link AIPCollection}
     */
    @RequestMapping(value = AIP_BULK, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to retrieve a collection of aip corresponding to the given set of ids")
    @ResponseBody
    public ResponseEntity<AIPCollection> retrieveAipsBulk(@RequestBody @Valid @RegardsOaisUrnAsString Set<String> ipIds)
            throws EntityNotFoundException {
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
     * Retrieve the aip of provided ip id
     * @return the aip
     */
    @RequestMapping(value = ID_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "allow to retrieve a given aip metadata thanks to its ipId")
    @ResponseBody
    public ResponseEntity<AIP> retrieveAip(@PathVariable(name = "ip_id") String ipId) throws EntityNotFoundException {
        return new ResponseEntity<>(aipService.retrieveAip(ipId), HttpStatus.OK);
    }

    /**
     * Add tags to an aip, represented by its ip id
     */
    @RequestMapping(value = TAG_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to add multiple tags to a given aip")
    @ResponseBody
    public ResponseEntity<Void> addTags(@PathVariable(name = "ip_id") String ipId, @RequestBody Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInconsistentIdentifierException {
        aipService.addTags(ipId, tagsToAdd);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Add tags to a list of aips
     */
    @RequestMapping(value = TAG_MANAGEMENT_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to add multiple tags to several aips")
    @ResponseBody
    public ResponseEntity<Void> addTagsByQuery(@RequestBody AddAIPTagsFilters request) {
        aipService.addTagsByQuery(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Remove tags from a given aip, represented by its ip id
     */
    @RequestMapping(value = TAG_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allow to remove multiple tags to a given aip")
    @ResponseBody
    public ResponseEntity<Void> removeTags(@PathVariable(name = "ip_id") String ipId,
            @RequestBody Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInconsistentIdentifierException {
        aipService.removeTags(ipId, tagsToRemove);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * Remove tags from a list of aips
     */
    @RequestMapping(value = TAG_DELETION_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to remove multiple tags to several aips")
    @ResponseBody
    public ResponseEntity<Void> removeTagsByQuery(@RequestBody RemoveAIPTagsFilters request) {
        aipService.removeTagsByQuery(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /**
     * Retrieve common tags from a list of aips
     */
    @RequestMapping(value = TAG_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "allow to search tags used by aips")
    public ResponseEntity<List<String>> retrieveAIPTags(@RequestBody AIPQueryFilters request) {
        List<String> aipTags = aipService.retrieveAIPTagsByQuery(request);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }


    /**
     * Update an aip, represented by its ip id, thanks to the provided pojo
     * @return updated aip
     */
    @RequestMapping(value = ID_PATH, method = RequestMethod.PUT,
            consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    @ResourceAccess(description = "allow to update a given aip metadata")
    @ResponseBody
    public ResponseEntity<AIP> updateAip(@PathVariable(name = "ip_id") String ipId, @RequestBody @Valid AIP updated)
            throws EntityInconsistentIdentifierException, EntityOperationForbiddenException, EntityNotFoundException {
        return new ResponseEntity<>(aipService.updateAip(ipId, updated), HttpStatus.OK);
    }

    /**
     * Delete an aip, represented by its ip id
     */
    @RequestMapping(value = ID_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allow to delete a given aip", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<String> deleteAip(@PathVariable(name = "ip_id") String ipId) throws ModuleException {
        Set<StorageDataFile> notSuppressible = aipService.deleteAip(ipId);
        if (notSuppressible.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            StringJoiner sj = new StringJoiner(", ",
                    "This aip could not be deleted because at least one of his files has not be handle by the storage process: ",
                    ".");
            notSuppressible.stream().map(sdf -> sdf.getAipEntity()).forEach(aipEntity -> sj.add(aipEntity.getIpId()));
            return new ResponseEntity<>(sj.toString(), HttpStatus.CONFLICT);
        }
    }

    /**
     * Retrieve all aips which are tagged by the provided tag
     * @return aips tagged by the tag
     */
    @RequestMapping(value = TAG, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve a collection of AIP according to a tag")
    @ResponseBody
    public ResponseEntity<AIPCollection> retrieveAipsByTag(@PathVariable("tag") String tag) {
        AIPCollection aipCollection = new AIPCollection();
        aipCollection.addAll(aipService.retrieveAipsByTag(tag));
        return ResponseEntity.ok(aipCollection);
    }

    /**
     * Retrieve the history of events that occurred on a given aip, represented by its ip id
     * @return the history of events that occurred to the aip
     */
    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "send the history of event occurred on each data file of the specified AIP")
    @ResponseBody
    public ResponseEntity<List<Event>> retrieveAIPHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId)
            throws ModuleException {
        List<Event> history = aipService.retrieveAIPHistory(pIpId);
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @RequestMapping(value = VERSION_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of versions of an aip through their ip ids")
    public ResponseEntity<List<String>> retrieveAIPVersionHistory(
            @PathVariable("ip_id") @Valid UniformResourceName pIpId, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws EntityNotFoundException {
        List<String> versions = aipService.retrieveAIPVersionHistory(pIpId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("ip_id") String aipId,
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
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        Resource<AIP> resource = resourceService.toResource(pElement);
        resourceService
                .addLink(resource, this.getClass(), "retrieveAIPs", LinkRels.LIST,
                        MethodParamFactory.build(AIPState.class),
                        MethodParamFactory.build(OffsetDateTime.class),
                        MethodParamFactory.build(OffsetDateTime.class),
                        MethodParamFactory.build(List.class),
                        MethodParamFactory.build(String.class),
                        MethodParamFactory.build(Pageable.class),
                        MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource, this.getClass(), "retrieveAip", LinkRels.SELF,
                MethodParamFactory.build(String.class, pElement.getId().toString()));
        resourceService.addLink(resource, this.getClass(), "storeRetryUnit", "retry",
                MethodParamFactory.build(String.class, pElement.getId().toString()));
        return resource;
    }

    /**
     * Handles any changes that should occurred between private rs-storage information and how the rest of the world
     * should see them.
     * For example, change URL from file:// to http://[project_host]/[gateway prefix]/rs-storage/...
     */
    private void toPublicDataFile(DataFileDto dataFile, AIP owningAip) throws MalformedURLException {
        // Lets reconstruct the public url of rs-storage
        // First lets get the public hostname from rs-admin-instance
        FeignSecurityManager.asSystem();
        String projectHost = projectsClient.retrieveProject(runtimeTenantResolver.getTenant()).getBody().getContent()
                .getHost();
        FeignSecurityManager.reset();
        // now lets add it the gateway prefix and the microservice name and the endpoint path to it
        StringBuilder sb = new StringBuilder();
        sb.append(projectHost);
        sb.append("/");
        sb.append(gatewayPrefix);
        sb.append("/");
        sb.append(microserviceName);
        sb.append(AIP_PATH);
        sb.append(DOWNLOAD_AIP_FILE.replaceAll("\\{ip_id\\}", owningAip.getId().toString())
                .replaceAll("\\{checksum\\}", dataFile.getChecksum()));
        URL downloadUrl = new URL(sb.toString());
        dataFile.setUrl(downloadUrl);
    }

}
