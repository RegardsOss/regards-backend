package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.RolesFallback;
import fr.cnes.regards.modules.accessRights.signature.IRolesSignature;

@FeignClient(value = "rs-admin", fallback = RolesFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface RolesClient extends IRolesSignature {

}