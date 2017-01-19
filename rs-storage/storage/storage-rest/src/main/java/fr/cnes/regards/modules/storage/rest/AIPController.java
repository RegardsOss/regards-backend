/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityCorruptByNetworkException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.service.IAIPService;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

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

    public static final String AIP_PATH = "/aip";

    public static final String ID_PATH = AIP_PATH + "/{ipId}";

    public static final String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    public static final String ID_OBJECT_LINK_PATH = OBJECT_LINK_PATH + "/{objectLinkid}";

    public static final String VERSION_PATH = ID_PATH + "/versions";

    public static final String HISTORY_PATH = ID_PATH + "/history";

    public static final String TAG_PATH = ID_PATH + "/tags";

    public static final String TAG = TAG_PATH + "{tag}";

    public static final String QUICK_LOOK = ID_PATH + "/quicklook";

    public static final String THUMB_NAIL = ID_PATH + "/thumbnail";

    public static final String TAGS_PATH = AIP_PATH + "/tags";

    public static final String TAGS_VALUE_PATH = TAGS_PATH + "/{tag}";

    public static final String OBJECT_LINKS_ID_PATH = AIP_PATH + "/objectLinks/{objectLinkid}";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of all aips")
    public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) LocalDateTime pFrom,
            @RequestParam(name = "to", required = false) LocalDateTime pTo, final Pageable pPageable,
            final PagedResourcesAssembler<AIP> pAssembler) {
        Page<AIP> aips = aipService.retrieveAIPs(pState, pFrom, pTo, pPageable);
        return new ResponseEntity<>(toPagedResources(aips, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(value = AIP_PATH, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "validate and create the specified AIP")
    public HttpEntity<Long> createAIP(@RequestHeader(name = "Content-MD5", required = true) String pChecksum,
            @RequestBody @Valid List<AIP> pAIP)
            throws EntityCorruptByNetworkException, NoSuchAlgorithmException, IOException {
        // String calculatedChecksum = "";
        // if (!validateChecksum(pChecksum, pAIP, calculatedChecksum)) {
        // throw new EntityCorruptByNetworkException(pAIP.getIpId().toString(), calculatedChecksum, pChecksum);
        // }
        // Long jobId = aipService.create(pAIP);
        // return new ResponseEntity<>(jobId, HttpStatus.SEE_OTHER);
        // FIXME
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files of a specified aip")
    public HttpEntity<List<DataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid UniformResourceName pIpId)
            throws EntityNotFoundException {
        List<DataObject> files = aipService.retrieveAIPFiles(pIpId);
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files of a specified aip")
    public HttpEntity<List<String>> retrieveAIPVersionHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId,
            final Pageable pPageable, final PagedResourcesAssembler<AIP> pAssembler) throws EntityNotFoundException {
        List<String> versions = aipService.retrieveAIPVersionHistory(pIpId);
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }

    public boolean validateChecksum(String pChecksum, Object pObject, String pCalculatedChecksum)
            throws NoSuchAlgorithmException, IOException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(pObject);
        oos.close();
        byte[] digest = md.digest(baos.toByteArray());
        pCalculatedChecksum = new String(digest);
        return pChecksum.equals(pCalculatedChecksum);

    }

    @Override
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }
}
