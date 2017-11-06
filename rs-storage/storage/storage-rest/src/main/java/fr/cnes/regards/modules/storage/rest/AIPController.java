/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.oais.urn.validator.RegardsOaisUrnAsString;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * REST controller handling request about {@link AIP}s
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@ModuleInfo(name = "storage", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(AIPController.AIP_PATH)
public class AIPController implements IResourceController<AIP> {

    public static final String RETRY_STORE_PATH = "/retry";

    public static final String AIP_PATH = "/aips";

    public static final String AIP_BULK = AIP_PATH + "/bulk";

    public static final String PREPARE_DATA_FILES = "/dataFiles";

    public static final String ID_PATH = "/{ip_id}";

    public static final String IP_ID_RETRY_STORE_PATH = ID_PATH + RETRY_STORE_PATH;

    public static final String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    public static final String ID_OBJECT_LINK_PATH = OBJECT_LINK_PATH + "/{objectLinkid}";

    public static final String VERSION_PATH = ID_PATH + "/versions";

    public static final String HISTORY_PATH = ID_PATH + "/history";

    public static final String TAG_PATH = ID_PATH + "/tags";

    public static final String TAG = TAG_PATH + "/{tag}";

    public static final String QUICK_LOOK = ID_PATH + "/quicklook";

    public static final String THUMB_NAIL = ID_PATH + "/thumbnail";

    public static final String TAGS_PATH = "/tags";

    public static final String TAGS_VALUE_PATH = "/{tag}";

    public static final String OBJECT_LINKS_ID_PATH = "/objectLinks/{objectLinkid}";

    public static final String DOWLOAD_AIP_FILE = "/{ip_id}/files/{checksum}";

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPController.class);

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of all aips")
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws ModuleException {
        Page<AIP> aips = aipService.retrieveAIPs(pState, pFrom, pTo, pPageable);
        return new ResponseEntity<>(toPagedResources(aips, pAssembler), HttpStatus.OK);
    }

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

    @RequestMapping(method = RequestMethod.POST, value = IP_ID_RETRY_STORE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Retry to store given aip, threw its ip id")
    public ResponseEntity<RejectedAip> storeRetryUnit(@PathVariable("ip_id") String ipId) throws ModuleException {
        //we ask for one AIP to be stored, so we can only have one rejected aip in counter part
        ResponseEntity<List<RejectedAip>> listResponse = storeRetry(Sets.newHashSet(ipId));
        if (listResponse.getBody().isEmpty()) {
            return new ResponseEntity<>(listResponse.getStatusCode());
        } else {
            return new ResponseEntity<>(listResponse.getBody().get(0), listResponse.getStatusCode());
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    @ResponseBody
    @ResourceAccess(description = "validate and storeAndCreate the specified AIP")
    public ResponseEntity<List<RejectedAip>> store(@RequestBody @Valid AIPCollection aips) throws ModuleException {
        //lets validate the inputs and get those in error
        List<RejectedAip> rejectedAips = aipService.applyCreationChecks(aips);
        //if there is some errors, lets handle the issues
        if (!rejectedAips.isEmpty()) {
            //now lets remove the inputs in error from aips to store
            Set<String> rejectedIpIds = rejectedAips.stream().map(ra -> ra.getIpId()).collect(Collectors.toSet());
            Set<AIP> aipNotToBeStored = aips.getFeatures().stream()
                    .filter(aip -> rejectedIpIds.contains(aip.getId().toString())).collect(Collectors.toSet());
            aips.getFeatures().removeAll(aipNotToBeStored);
            //if there is nothing more to be stored, UNPROCESABLE ENTITY
            if (aips.getFeatures().isEmpty()) {
                return new ResponseEntity<>(rejectedAips, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            aipService.storeAndCreate(Sets.newHashSet(aips.getFeatures()));
            return new ResponseEntity<>(rejectedAips, HttpStatus.PARTIAL_CONTENT);
        }
        aipService.storeAndCreate(Sets.newHashSet(aips.getFeatures()));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "delete AIPs associated to the given SIP, given threw its ip id")
    public ResponseEntity<Void> deleteAipFromSip(@RequestParam("sip_ip_id") String sipIpId) throws ModuleException {
        aipService.deleteAipFromSip(sipIpId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files metadata of a specified aip")
    public ResponseEntity<Set<OAISDataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId)
            throws ModuleException {
        Set<OAISDataObject> files = aipService.retrieveAIPFiles(UniformResourceName.fromString(pIpId));
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(
            description = "allows to request that files are made available for downloading, return the list of file already available via their checksums")
    public ResponseEntity<AvailabilityResponse> makeFilesAvailable(@RequestBody AvailabilityRequest availabilityRequest)
            throws ModuleException {
        return ResponseEntity.ok(aipService.loadFiles(availabilityRequest));
    }

    @RequestMapping(value = AIP_BULK, method = RequestMethod.POST)
    @ResourceAccess(description = "allows to retrieve a collection of aip corresponding to the given set of ids")
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
            //Otherwise, HttpStatus PARTIAL_CONTENT(206)
            return new ResponseEntity<>(aipCollection, HttpStatus.PARTIAL_CONTENT);
        }
    }

    @RequestMapping(value = ID_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "allows to retrieve a given aip metadata thabnks to its ipId")
    @ResponseBody
    public ResponseEntity<AIP> retrieveAip(@PathVariable(name = "ip_id") String ipId) throws EntityNotFoundException {
        return new ResponseEntity<>(aipService.retrieveAip(ipId), HttpStatus.OK);
    }

    @RequestMapping(value = ID_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "allows to update a given aip metadata")
    @ResponseBody
    public ResponseEntity<AIP> updateAip(@PathVariable(name = "ip_id") String ipId, @RequestBody @Valid AIP updated)
            throws EntityInconsistentIdentifierException, EntityOperationForbiddenException, EntityNotFoundException {
        return new ResponseEntity<>(aipService.updateAip(ipId, updated), HttpStatus.OK);
    }

    @RequestMapping(value = ID_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allows to update a given aip metadata", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<Void> deleteAip(@PathVariable(name = "ip_id") String ipId) throws ModuleException {
        aipService.deleteAip(ipId);
        return (ResponseEntity<Void>) ResponseEntity.noContent();
    }

    @RequestMapping(value = TAG, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve a collection of AIP according to a tag")
    @ResponseBody
    public ResponseEntity<AIPCollection> retrieveAipsByTag(@PathVariable("tag") String tag) {
        AIPCollection aipCollection = new AIPCollection();
        aipCollection.addAll(aipService.retrieveAipsByTag(tag));
        return ResponseEntity.ok(aipCollection);
    }

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "send the history of event occured on each data file of the specified AIP")
    @ResponseBody
    public ResponseEntity<List<Event>> retrieveAIPHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId)
            throws ModuleException {
        List<Event> history = aipService.retrieveAIPHistory(pIpId);
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @RequestMapping(value = VERSION_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files of a specified aip")
    public ResponseEntity<List<String>> retrieveAIPVersionHistory(
            @PathVariable("ip_id") @Valid UniformResourceName pIpId, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) throws EntityNotFoundException {
        List<String> versions = aipService.retrieveAIPVersionHistory(pIpId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    @RequestMapping(path = DOWLOAD_AIP_FILE, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "Dowload one file from a given AIP by checksum.")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("ip_id") String aipId,
            @PathVariable("checksum") String checksum) throws ModuleException, IOException {
        // Retrieve file locale path
        Optional<DataFile> dataFile = aipService.getAIPDataFile(aipId, checksum);
        if (dataFile.isPresent()) {
            File file = new File(dataFile.get().getUrl().getPath());
            InputStreamResource isr = new InputStreamResource(new FileInputStream(file));
            Long fileSize = file.length();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(fileSize);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachement", dataFile.get().getName());
            return new ResponseEntity<>(isr, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
    }

    @Override
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        Resource<AIP> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveAIPs",
                                LinkRels.LIST,
                                MethodParamFactory.build(AIPState.class),
                                MethodParamFactory.build(OffsetDateTime.class),
                                MethodParamFactory.build(OffsetDateTime.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveAIP",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getId().toString()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "storeRetryUnit",
                                "retry",
                                MethodParamFactory.build(String.class, pElement.getId().toString()));
        return resource;
    }

}
