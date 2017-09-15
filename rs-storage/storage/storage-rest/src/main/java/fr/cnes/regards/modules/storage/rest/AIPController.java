/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.gson.Gson;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
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

    public static final String AIP_PATH = "/aips";

    public static final String PREPARE_DATA_FILES = "/dataFiles";

    public static final String ID_PATH = AIP_PATH + "/{ipId}";

    public static final String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    public static final String ID_OBJECT_LINK_PATH = OBJECT_LINK_PATH + "/{objectLinkid}";

    public static final String VERSION_PATH = ID_PATH + "/versions";

    public static final String HISTORY_PATH = ID_PATH + "/history";

    public static final String TAG_PATH = ID_PATH + "/tags";

    public static final String TAG = TAG_PATH + "/{tag}";

    public static final String QUICK_LOOK = ID_PATH + "/quicklook";

    public static final String THUMB_NAIL = ID_PATH + "/thumbnail";

    public static final String TAGS_PATH = AIP_PATH + "/tags";

    public static final String TAGS_VALUE_PATH = TAGS_PATH + "/{tag}";

    public static final String OBJECT_LINKS_ID_PATH = AIP_PATH + "/objectLinks/{objectLinkid}";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private Gson gson;

    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of all aips")
    public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) {
        Page<AIP> aips = aipService.retrieveAIPs(pState, pFrom, pTo, pPageable);
        return new ResponseEntity<>(toPagedResources(aips, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(value = AIP_PATH, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "validate and create the specified AIP")
    public HttpEntity<Set<UUID>> createAIP(@RequestBody @Valid Set<AIP> aips)
            throws ModuleException, NoSuchAlgorithmException {
        Set<UUID> jobIds = aipService.create(aips);
        return new ResponseEntity<>(jobIds, HttpStatus.SEE_OTHER);
    }

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files of a specified aip")
    public HttpEntity<List<DataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId)
            throws EntityNotFoundException {
        List<DataObject> files = aipService.retrieveAIPFiles(UniformResourceName.fromString(pIpId));
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(
            description = "allows to request that files are made available for downloading, return the list of file already available via their checksums")
    public HttpEntity<AvailabilityResponse> makeFilesAvailable(@RequestBody AvailabilityRequest availabilityRequest) {
        return ResponseEntity.ok(aipService.loadFiles(availabilityRequest));
    }

    // @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    // @ResponseBody
    // @ResourceAccess(description = "send the history of event occured on each data file of the specified AIP")
    // public HttpEntity<Map<String, List<Event>>> retrieveAIPHistory(
    // @PathVariable("ip_id") @Valid UniformResourceName pIpId) throws EntityNotFoundException {
    // Map<String, List<Event>> history = aipService.retrieveAIPHistory(pIpId);
    // return new ResponseEntity<>(history, HttpStatus.OK);
    // }

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files of a specified aip")
    public HttpEntity<List<String>> retrieveAIPVersionHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId,
            final Pageable pPageable, final PagedResourcesAssembler<AIP> pAssembler) throws EntityNotFoundException {
        List<String> versions = aipService.retrieveAIPVersionHistory(pIpId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    @Override
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }


}
