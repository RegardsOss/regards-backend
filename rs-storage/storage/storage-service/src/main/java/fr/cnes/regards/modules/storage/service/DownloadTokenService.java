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
package fr.cnes.regards.modules.storage.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.dao.IDownloadTokenRepository;
import fr.cnes.regards.modules.storage.domain.database.DownloadToken;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class DownloadTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadTokenService.class);

    @Autowired
    private IDownloadTokenRepository downTokenRepo;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Remove all expired download tokens
     */
    public void purgeTokens() {
        downTokenRepo.deleteByExpirationDateBefore(OffsetDateTime.now());
    }

    /**
     * Generate a download token for the file associated to the given checksum
     *
     * @param checksum
     * @return download token
     */
    public String createDownloadToken(String checksum) {
        String newToken = UUID.randomUUID().toString();
        downTokenRepo.save(DownloadToken.build(newToken, checksum, OffsetDateTime.now().plusHours(1)));
        return newToken;
    }

    /**
     * Check if given token is valid to download the file associated to the given checksum.
     *
     * @param checksum
     * @param token
     */
    public boolean checkToken(String checksum, String token) {
        boolean accessGranted = downTokenRepo.existsByChecksumAndTokenAndExpirationDateAfter(checksum,
                                                                                             token,
                                                                                             OffsetDateTime.now());
        if (!accessGranted) {
            LOGGER.error("Access denied to file {}. Token {} is no longer valid", checksum, token);
        }
        return accessGranted;
    }

    /**
     * Generate a public download URL for the file associated to the given Checksum
     *
     * @param checksum
     * @return download url
     * @throws ModuleException if the Eureka server is not reachable
     */
    public String generateDownloadUrl(String checksum) throws ModuleException {
        Optional<ServiceInstance> instance = discoveryClient.getInstances(applicationName).stream().findFirst();
        if (instance.isPresent()) {
            String host = instance.get().getUri().toString();
            String path = Paths.get(FileDownloadService.FILES_PATH, FileDownloadService.DOWNLOAD_TOKEN_PATH).toString();
            String p = path.replace("{checksum}", checksum);
            p = p.charAt(0) == '/' ? p.replaceFirst("/", "") : p;
            return String.format("%s/%s?scope=%s&%s=%s",
                                 host,
                                 p,
                                 tenantResolver.getTenant(),
                                 FileDownloadService.TOKEN_PARAM,
                                 createDownloadToken(checksum));
        } else {
            throw new ModuleException("Error getting storage microservice address from eureka client");
        }
    }
}
