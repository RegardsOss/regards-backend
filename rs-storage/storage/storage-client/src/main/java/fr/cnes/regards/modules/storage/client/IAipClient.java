/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.client;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
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
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.RejectedAip;

/**
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@RestClient(name = "rs-storage")
@RequestMapping(value = IAipClient.AIP_PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAipClient {

    String AIP_PATH = "/aips";

    String RETRY_STORE_PATH = "/retry";

    String PREPARE_DATA_FILES = "/dataFiles";

    String ID_PATH = "/{ip_id}";

    String IP_ID_RETRY_STORE_PATH = ID_PATH + RETRY_STORE_PATH;

    String OBJECT_LINK_PATH = ID_PATH + "/objectlinks";

    String ID_OBJECT_LINK_PATH = OBJECT_LINK_PATH + "/{objectLinkid}";

    String VERSION_PATH = ID_PATH + "/versions";

    String HISTORY_PATH = ID_PATH + "/history";

    String TAG_PATH = ID_PATH + "/tags";

    String TAG = TAG_PATH + "/{tag}";

    String QUICK_LOOK = ID_PATH + "/quicklook";

    String THUMB_NAIL = ID_PATH + "/thumbnail";

    String TAGS_PATH = "/tags";

    String TAGS_VALUE_PATH = TAGS_PATH + "/{tag}";

    String OBJECT_LINKS_ID_PATH = "/objectLinks/{objectLinkid}";

    String DOWLOAD_AIP_FILE = "/{ip_id}/files/{checksum}";

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(
            @RequestParam(name = "state", required = false) AIPState pState,
            @RequestParam(name = "from", required = false) OffsetDateTime pFrom,
            @RequestParam(name = "to", required = false) OffsetDateTime pTo, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    public ResponseEntity<List<RejectedAip>> store(@RequestBody @Valid AIPCollection aips);

    @RequestMapping(method = RequestMethod.POST, value = RETRY_STORE_PATH)
    public ResponseEntity<List<RejectedAip>> storeRetry(@RequestBody @Valid Set<String> aipIpIds);

    @RequestMapping(method = RequestMethod.POST, value = IP_ID_RETRY_STORE_PATH)
    public ResponseEntity<RejectedAip> storeRetryUnit(@PathVariable("ip_id") String ipId);

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAipFromSip(@RequestParam("sip_ip_id") String sipIpId);

    @RequestMapping(value = OBJECT_LINK_PATH, method = RequestMethod.GET)
    public ResponseEntity<List<OAISDataObject>> retrieveAIPFiles(@PathVariable("ip_id") @Valid String pIpId);

    @RequestMapping(value = HISTORY_PATH, method = RequestMethod.GET)
    public ResponseEntity<List<String>> retrieveAIPVersionHistory(
            @PathVariable("ip_id") @Valid UniformResourceName pIpId, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @RequestMapping(path = PREPARE_DATA_FILES, method = RequestMethod.POST)
    public ResponseEntity<AvailabilityResponse> makeFilesAvailable(
            @RequestBody AvailabilityRequest availabilityRequest);

    @RequestMapping(path = DOWLOAD_AIP_FILE, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Response downloadFile(@PathVariable("ip_id") String aipId, @PathVariable("checksum") String checksum);
}
