package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.ProjectUsersFallback;
import fr.cnes.regards.modules.accessRights.signature.RolesSignature;

@FeignClient(name = "roles", fallback = ProjectUsersFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface RolesClient extends RolesSignature {

}