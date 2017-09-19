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
import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestClient(name = "rs-storage")
@RequestMapping(value = IAipClient.AIP_PATH)
public interface IAipClient {

    String AIP_PATH = "/aips";

    String ID_PATH = AIP_PATH + "/{ipId}";

    String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    String ID_OBJECT_LINK_PATH = OBJECT_LINK_PATH + "/{objectLinkid}";

    String VERSION_PATH = ID_PATH + "/versions";

    String HISTORY_PATH = ID_PATH + "/history";

    String TAG_PATH = ID_PATH + "/tags";

    String TAG = TAG_PATH + "/{tag}";

    String QUICK_LOOK = ID_PATH + "/quicklook";

    String THUMB_NAIL = ID_PATH + "/thumbnail";

    String TAGS_PATH = AIP_PATH + "/tags";

    String TAGS_VALUE_PATH = TAGS_PATH + "/{tag}";

    String OBJECT_LINKS_ID_PATH = AIP_PATH + "/objectLinks/{objectLinkid}";

    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @RequestMapping(value = AIP_PATH, method = RequestMethod.POST)
    HttpEntity<Set<UUID>> createAIP(@RequestBody @Valid Set<AIP> aips);

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    HttpEntity<List<DataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid UniformResourceName pIpId);

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    HttpEntity<List<String>> retrieveAIPVersionHistory(@PathVariable("ip_id") @Valid UniformResourceName pIpId,
            @RequestParam("page") int pPage, @RequestParam("size") int pSize);

}
