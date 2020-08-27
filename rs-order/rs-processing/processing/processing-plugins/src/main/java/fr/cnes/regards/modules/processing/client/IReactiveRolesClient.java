package fr.cnes.regards.modules.processing.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(name = "rs-admin")
@Headers({
    "Content-Type: application/json",
    "Accept: application/json"
})
public interface IReactiveRolesClient {

    String ROLE_MAPPING = "/{role_name}";
    String SHOULD_ACCESS_TO_RESOURCE = "/include" + ROLE_MAPPING;


    @RequestLine("GET " + SHOULD_ACCESS_TO_RESOURCE)
    @Headers({
            HttpConstants.AUTHORIZATION + ": " + HttpConstants.BEARER + " {auth_token}"
    })
    Mono<Boolean> shouldAccessToResourceRequiring(
            @Param("role_name") String roleName,
            @Param("auth_token") String authToken
    );

}
