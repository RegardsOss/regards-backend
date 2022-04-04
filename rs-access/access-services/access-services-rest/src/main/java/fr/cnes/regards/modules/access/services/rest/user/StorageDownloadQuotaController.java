/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaRestClient.*;

@RestController
public class StorageDownloadQuotaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDownloadQuotaController.class);

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Client handling storage quotas
     */
    @Autowired
    private IStorageRestClient storageClient;


    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get user download quota limits.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail
    ) throws ModuleException {
        return wrapStorageErrorForFrontend(
            () -> {FeignSecurityManager
                    .asUser(authenticationResolver.getUser(), RoleAuthority
                            .getSysRole(appName));return storageClient.getQuotaLimits(userEmail);},
            () -> new DownloadQuotaLimitsDto(authResolver.getUser(), null, null)
        );
    }

    @PutMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Update user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail,
        @Valid @RequestBody DownloadQuotaLimitsDto quotaLimits
    ) throws ModuleException {
        return wrapStorageErrorForFrontend(
            () -> {FeignSecurityManager
                    .asUser(authenticationResolver.getUser(), RoleAuthority.getSysRole(appName));return storageClient.upsertQuotaLimits(userEmail, quotaLimits);},
            () -> new DownloadQuotaLimitsDto(authResolver.getUser(), null, null)
        );
    }

    @GetMapping(path = PATH_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current user download quota limits.", role = DefaultRole.PUBLIC)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() throws ModuleException {
        return wrapStorageErrorForFrontend(
            () -> storageClient.getQuotaLimits(),
            () -> new DownloadQuotaLimitsDto(authResolver.getUser(), null, null)
        );
    }

    @GetMapping(path = PATH_CURRENT_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current download quota values for current user.", role = DefaultRole.PUBLIC)
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() throws ModuleException {
        return wrapStorageErrorForFrontend(
            () -> storageClient.getCurrentQuotas(),
            () -> new UserCurrentQuotas(authResolver.getUser())
        );
    }

    private <V> ResponseEntity<V> wrapStorageErrorForFrontend(
        Supplier<ResponseEntity<V>> action,
        Supplier<V> orElse
    ) throws ModuleException {
        return Try.ofSupplier(action)
            // response should be remapped because of a "bug" somewhere in spring that does not treat headers as case-insentive while feign does
            .map(response->new ResponseEntity<>(response.getBody(), response.getStatusCode()))
            // add FeignSecurityManager.reset call so that security is properly handled in case quotaSupplier usurp identity. Otherwise, reset just set back the value per default
            .andFinally(FeignSecurityManager::reset)
            // special value for frontend if any error on storage or storage not deploy
            .onFailure(e -> LOGGER.debug("Failed to query rs-storage for quotas.", e))
            .orElse(() -> Try.success(orElse.get())
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK)))
            .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }
}
