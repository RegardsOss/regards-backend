package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.AccessesFallback;
import fr.cnes.regards.modules.accessRights.signature.AccessesSignature;

@FeignClient(name = "accesses", fallback = AccessesFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface AccessesClient extends AccessesSignature {

}