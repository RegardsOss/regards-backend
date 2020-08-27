package fr.cnes.regards.modules.processing.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.framework.security.utils.HttpConstants.AUTHORIZATION;
import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;

@ReactiveFeignClient(name = "rs-admin")
public interface IReactiveRolesClient {

    String ROLE_MAPPING = "/{role_name}";
    String SHOULD_ACCESS_TO_RESOURCE = "/include" + ROLE_MAPPING;

    @GetMapping(path = SHOULD_ACCESS_TO_RESOURCE, produces = APPLICATION_JSON)
    Mono<Boolean> shouldAccessToResourceRequiring(
            @PathVariable("role_name") String roleName,
            @RequestHeader(AUTHORIZATION) String authToken
    );

}
