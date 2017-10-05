package fr.cnes.regards.modules.storage.client;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestClient(name = "rs-storage")
@RequestMapping(value = IAipClient.AIP_PATH)
public interface IAipClient {

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

    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @RequestMapping(value = AIP_PATH, method = RequestMethod.POST)
    public HttpEntity<Set<UUID>> createAIP(@RequestBody @Valid Set<AIP> aips);

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    public HttpEntity<List<OAISDataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid UniformResourceName pIpId);

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    public HttpEntity<List<String>> retrieveAIPVersionHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId,
            @RequestParam("page") int pPage, @RequestParam("size") int pSize);

    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    public HttpEntity<AvailabilityResponse> makeFilesAvailable(@RequestBody AvailabilityRequest availabilityRequest);

    public HttpEntity<Void> downloadFile(String aipId, String checksum);
}
