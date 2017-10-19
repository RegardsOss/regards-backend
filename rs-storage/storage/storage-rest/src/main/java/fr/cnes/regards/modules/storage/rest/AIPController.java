/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
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

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPController.class);

    public static final String AIP_PATH = "/aips";

    public static final String PREPARE_DATA_FILES = "/dataFiles";

    public static final String ID_PATH = "/{ip_id}";

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

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
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

    @RequestMapping(value = AIP_PATH, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "validate and store the specified AIP")
    public ResponseEntity<Set<UUID>> store(@RequestBody @Valid AIPCollection aips)
            throws ModuleException, NoSuchAlgorithmException {
        Set<UUID> jobIds = aipService.store(Sets.newHashSet(aips.getFeatures()));
        return new ResponseEntity<>(jobIds, HttpStatus.OK);
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

    @RequestMapping(value = ID_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "allows to update a given aip metadata")
    @ResponseBody
    public ResponseEntity<AIP> updateAip(@PathVariable(name = "ip_id") String ipId, @RequestBody @Valid AIP updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException,
            EntityNotFoundException {
        return new ResponseEntity<AIP>(aipService.updateAip(ipId, updated), HttpStatus.OK);
    }

    @RequestMapping(value = ID_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "allows to update a given aip metadata", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<Void> deleteAip(@PathVariable(name = "ip_id") String ipId) throws ModuleException {
        aipService.deleteAip(ipId);
        return (ResponseEntity<Void>) ResponseEntity.noContent();
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
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }

}
