/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
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
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * REST controller handling request about {@link AIP}s
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
@ModuleInfo(name = "storage", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(AIPController.AIP_PATH)
public class AIPController implements IResourceController<AIP> {

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
    public static final String DOWLOAD_AIP_FILE = "/{ip_id}/files/{checksum}";

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPController.class);

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

    /**
     * Retrieve a page of aip metadata according to the given parameters
     * @param pState state the aips should be in
     * @param pFrom date after which the aip should have been added to the system
     * @param pTo date before which the aip should have been added to the system
     * @return page of aip metadata respecting the constraints
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send a page of aips")
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws ModuleException {
        Page<AIP> aips = aipService.retrieveAIPs(pState, pFrom, pTo, pPageable);
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
        PagedResources<AipDataFiles> aipDataFiles = new PagedResources<>(content,
                                                                         new PagedResources.PageMetadata(page.getSize(),
                                                                                                         page.getNumber(),
                                                                                                         page.getTotalElements(),
                                                                                                         page.getTotalPages()));
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
    @ResponseBody
    @ResourceAccess(description = "validate and store the specified AIP")
    public ResponseEntity<List<RejectedAip>> store(@RequestBody AIPCollection aips) throws ModuleException {
        // lets validate the inputs and get those in error
        List<RejectedAip> rejectedAips = aipService.applyCreationChecks(aips);
        // if there is some errors, lets handle the issues
        if (!rejectedAips.isEmpty()) {
            // now lets remove the inputs in error from aips to store
            Set<String> rejectedIpIds = rejectedAips.stream().map(ra -> ra.getIpId()).collect(Collectors.toSet());
            Set<AIP> aipNotToBeStored = aips.getFeatures().stream()
                    .filter(aip -> rejectedIpIds.contains(aip.getId().toString())).collect(Collectors.toSet());
            aips.getFeatures().removeAll(aipNotToBeStored);
            // if there is nothing more to be stored, UNPROCESABLE ENTITY
            if (aips.getFeatures().isEmpty()) {
                return new ResponseEntity<>(rejectedAips, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            aipService.storeAndCreate(Sets.newHashSet(aips.getFeatures()));
            return new ResponseEntity<>(rejectedAips, HttpStatus.PARTIAL_CONTENT);
        }
        aipService.storeAndCreate(Sets.newHashSet(aips.getFeatures()));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Delete aips that are associated to at least one of the provided sips, represented by their ip id
     * @return SIP for which the deletion could not be made
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "delete AIPs associated to the given SIP, given threw its ip id")
    public ResponseEntity<List<RejectedSip>> deleteAipFromSips(@RequestParam("sip_ip_id") Set<String> sipIpIds)
            throws ModuleException {
        List<RejectedSip> notHandledSips = Lists.newArrayList();
        for (String sipIpId : sipIpIds) {
            try {
                aipService.deleteAipFromSip(sipIpId);
            } catch (ModuleException e) {
                LOG.error(e.getMessage(), e);
                notHandledSips.add(new RejectedSip(sipIpId, e));
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
    public ResponseEntity<Set<OAISDataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId)
            throws ModuleException {
        Set<OAISDataObject> files = aipService.retrieveAIPFiles(UniformResourceName.fromString(pIpId));
        return new ResponseEntity<>(files, HttpStatus.OK);
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
     * Remove tags from a given aip, represented by its ip id
     */
    @RequestMapping(value = TAG_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allow to remove multiple tags to a given aip")
    @ResponseBody
    public ResponseEntity<Void> removeTags(@PathVariable(name = "ip_id") String ipId,
            @RequestBody Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInconsistentIdentifierException {
        aipService.removeTags(ipId, tagsToRemove);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
    @ResourceAccess(description = "allow to update a given aip metadata", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<Void> deleteAip(@PathVariable(name = "ip_id") String ipId) throws ModuleException {
        aipService.deleteAip(ipId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
    @ResourceAccess(description = "send the list of versions of an aip threw there ip ids")
    public ResponseEntity<List<String>> retrieveAIPVersionHistory(
            @PathVariable("ip_id") @Valid UniformResourceName pIpId, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws EntityNotFoundException {
        List<String> versions = aipService.retrieveAIPVersionHistory(pIpId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    @RequestMapping(path = DOWLOAD_AIP_FILE, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("ip_id") String aipId,
            @PathVariable("checksum") String checksum) throws ModuleException, IOException {
        // Retrieve file locale path, 404 if aip not found or bad checksum or no storage plugin
        // 403 if user has not right
        Optional<StorageDataFile> dataFileOpt = aipService.getAIPDataFile(aipId, checksum);
        if (dataFileOpt.isPresent()) {
            StorageDataFile dataFile = dataFileOpt.get();
            File file = new File(dataFile.getUrl().getPath());
            InputStreamResource isr = new InputStreamResource(new FileInputStream(file));
            Long fileSize = file.length();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(fileSize);
            headers.setContentType(asMediaType(dataFile.getMimeType()));
            headers.setContentDispositionFormData("attachement;filename=", dataFile.getName());
            return new ResponseEntity<>(isr, headers, HttpStatus.OK);
        } else { // NEARLINE file not in cache
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
    }

    public static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }

    @Override
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        Resource<AIP> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveAIPs", LinkRels.LIST,
                                MethodParamFactory.build(AIPState.class),
                                MethodParamFactory.build(OffsetDateTime.class),
                                MethodParamFactory.build(OffsetDateTime.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource, this.getClass(), "retrieveAip", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getId().toString()));
        resourceService.addLink(resource, this.getClass(), "storeRetryUnit", "retry",
                                MethodParamFactory.build(String.class, pElement.getId().toString()));
        return resource;
    }

    /**
     * Handles any changes that should occurred between private rs-storage information and how the rest of the world should see them.
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
        sb.append(DOWLOAD_AIP_FILE.replaceAll("\\{ip_id\\}", owningAip.getId().toString())
                          .replaceAll("\\{checksum\\}", dataFile.getChecksum()));
        //don't forget to add the project into parameter scope
        sb.append("?scope=");
        sb.append(runtimeTenantResolver.getTenant());
        URL downloadUrl = new URL(sb.toString());
        dataFile.setUrl(downloadUrl);
    }

}
