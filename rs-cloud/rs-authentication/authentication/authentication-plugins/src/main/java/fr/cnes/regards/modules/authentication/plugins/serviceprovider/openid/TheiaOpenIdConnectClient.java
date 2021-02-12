package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.request.TheiaOpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdUserInfoResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestClient(name = "theia-open-id-connect", contextId = "theia-open-id-connect")
@RequestMapping(
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface TheiaOpenIdConnectClient {

    @PostMapping
    @ResponseBody
    ResponseEntity<TheiaOpenIdTokenResponse> token(@RequestBody TheiaOpenIdTokenRequest request);

    @GetMapping
    @ResponseBody
    ResponseEntity<TheiaOpenIdUserInfoResponse> userInfo();

    @PostMapping
    ResponseEntity<Void> revoke(@RequestParam("token") String token);
}
