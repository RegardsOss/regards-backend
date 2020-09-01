package fr.cnes.regards.modules.processing.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.framework.security.utils.HttpConstants.AUTHORIZATION;

@ReactiveFeignClient(name = "rs-admin")
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public interface IReactiveRolesClient {

    String ROLE_MAPPING = "/{role_name}";
    String SHOULD_ACCESS_TO_RESOURCE = "/include" + ROLE_MAPPING;

    @GetMapping(path = SHOULD_ACCESS_TO_RESOURCE)
    Mono<Boolean> shouldAccessToResourceRequiring(
            @PathVariable("role_name") String roleName,
            @RequestHeader(AUTHORIZATION) String authToken
    );

}
