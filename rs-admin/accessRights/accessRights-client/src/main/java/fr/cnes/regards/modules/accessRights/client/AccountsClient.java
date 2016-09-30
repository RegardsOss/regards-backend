package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.AccountsFallback;
import fr.cnes.regards.modules.accessRights.signature.AccountsSignature;

@FeignClient(name = "accounts", fallback = AccountsFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface AccountsClient extends AccountsSignature {

}
