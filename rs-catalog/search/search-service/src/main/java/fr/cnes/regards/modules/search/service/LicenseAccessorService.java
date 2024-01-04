/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Thomas Fache
 **/
@Service
public class LicenseAccessorService {

    private final LoadingCache<String, LicenseDTO> licenseCache;

    private final ILicenseClient licenceClient;

    private final IAuthenticationResolver authResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public LicenseAccessorService(ILicenseClient theLicenceClient,
                                  ISubscriber subscriber,
                                  IAuthenticationResolver authResolver,
                                  IRuntimeTenantResolver runtimeTenantResolver) {
        licenceClient = theLicenceClient;
        this.authResolver = authResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        licenseCache = initCache();
        subscriber.subscribeTo(LicenseEvent.class, new LicenseEventHandler());
    }

    private LoadingCache<String, LicenseDTO> initCache() {
        CacheLoader<String, LicenseDTO> retrieveLicenseLoader = new CacheLoader<String, LicenseDTO>() {

            @Override
            public LicenseDTO load(String userAndTenant) throws Exception {
                return retrieveLicense();
            }
        };
        return CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(retrieveLicenseLoader);
    }

    private LicenseDTO retrieveLicense() throws ModuleException {
        try {
            ResponseEntity<EntityModel<LicenseDTO>> licenseResponse = licenceClient.retrieveLicense();
            if (licenseResponse.getStatusCode() != HttpStatus.OK) {
                throw new ModuleException("License verification failed with status " + licenseResponse.getStatusCode());
            }
            EntityModel<LicenseDTO> licenseEntityModel = ResponseEntityUtils.extractBodyOrThrow(licenseResponse,
                                                                                                "Cannot retrieve license: response licenseEntityModel is empty");
            return licenseEntityModel.getContent();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ModuleException("License verification failed with error  " + e.getMessage(), e);
        }
    }

    public boolean currentUserHasAcceptedLicense() throws ExecutionException {
        return retrieveLicense(authResolver.getUser(), runtimeTenantResolver.getTenant()).isAccepted();
    }

    public LicenseDTO retrieveLicense(String forUser, String forTenant) throws ExecutionException {
        return licenseCache.get(cacheKey(forUser, forTenant));
    }

    public void acceptLicense(String forUser, String forTenant) throws ModuleException {
        ResponseEntity<EntityModel<LicenseDTO>> licenseResponse = licenceClient.acceptLicense();
        if (licenseResponse.getStatusCode() != HttpStatus.OK) {
            throw new ModuleException("License acceptation failed with status " + licenseResponse.getStatusCode());
        }
        LicenseDTO licenseDTO = ResponseEntityUtils.extractContentOrThrow(licenseResponse,
                                                                          "License acceptation failed : response body is empty");
        licenseCache.put(cacheKey(forUser, forTenant), licenseDTO);
    }

    private String cacheKey(String forUser, String forTenant) {
        return forUser + forTenant;
    }

    public void cleanCache() {
        // TODO temporary method for IT purpose.
        // It enables the cache cleaning
        // after each test.
        // It will be replaced with a notification mechanism from LicenseService
        licenseCache.invalidateAll();
    }

    private List<String> keysFor(String tenant) {
        return licenseCache.asMap().keySet().stream().filter(key -> key.endsWith(tenant)).collect(Collectors.toList());
    }

    public class LicenseEventHandler implements IHandler<LicenseEvent> {

        @Override
        public void handle(String tenant, LicenseEvent message) {
            if (message.getAction() == LicenseAction.ACCEPT) {
                licenseCache.put(cacheKey(message.getUser(), tenant), new LicenseDTO(true, message.getLicenseLink()));
            }
            if (message.getAction() == LicenseAction.RESET) {
                licenseCache.invalidateAll(keysFor(tenant));
            }
        }
    }
}
