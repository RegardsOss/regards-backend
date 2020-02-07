/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.dao.IDownloadTokenRepository;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.DownloadToken;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;

/**
 * Service to handle files download.<br>
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);

    public static final String FILES_PATH = "/files";

    public static final String DOWNLOAD_TOKEN_PATH = "/{checksum}/download/token";

    public static final String TOKEN_PARAM = "t";

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private IDownloadTokenRepository downTokenRepo;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private CacheService cachedFileService;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Download a file thanks to its checksum. If the file is stored in multiple storage location,
     * this method decide which one to retrieve by : <ul>
     *  <li>Only Files on an {@link IOnlineStorageLocation} location can be download</li>
     *  <li>Use the {@link StorageLocationConfiguration} configuration with the highest priority</li>
     * </ul>
     *
     * @param checksum Checksum of the file to download
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public DownloadableFile downloadFile(String checksum) throws ModuleException {
        // 1. Retrieve all the FileReference matching the given checksum
        Set<FileReference> fileRefs = fileRefService.search(checksum);
        if (fileRefs.isEmpty()) {
            throw new EntityNotFoundException(checksum, FileReferenceDTO.class);
        }
        Map<String, FileReference> storages = fileRefs.stream()
                .collect(Collectors.toMap(f -> f.getLocation().getStorage(), f -> f));
        // 2. get the storage location with the higher priority
        Optional<StorageLocationConfiguration> storageLocation = storageLocationConfService
                .searchActiveHigherPriority(storages.keySet());
        if (storageLocation.isPresent()) {
            PluginConfiguration conf = storageLocation.get().getPluginConfiguration();
            FileReference fileToDownload = storages.get(conf.getLabel());
            DownloadableFile df = new DownloadableFile(downloadFileReference(fileToDownload),
                    fileToDownload.getMetaInfo().getFileSize(), fileToDownload.getMetaInfo().getFileName(),
                    fileToDownload.getMetaInfo().getMimeType());
            return df;

        } else {
            throw new ModuleException(String
                    .format("No storage location configured for the given file reference (checksum %s). The file can not be download from %s.",
                            checksum, Arrays.toString(storages.keySet().toArray())));
        }
    }

    /**
     * Try to download the given {@link FileReference}.
     * @param fileToDownload
     * @return {@link InputStream} of the file
     * @throws ModuleException
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public InputStream downloadFileReference(FileReference fileToDownload) throws ModuleException {
        Optional<StorageLocationConfiguration> conf = storageLocationConfService
                .search(fileToDownload.getLocation().getStorage());
        if (conf.isPresent()) {
            switch (conf.get().getStorageType()) {
                case NEARLINE:
                    return download(fileToDownload);
                case ONLINE:
                    return downloadOnline(fileToDownload, conf.get());
                default:
                    break;
            }
        }
        throw new ModuleException(
                String.format("Unable to download file %s (checksum : %s) as its storage location %s is not available",
                              fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                              fileToDownload.getLocation().toString()));
    }

    /**
     * Download a file from an ONLINE storage location.
     * @param fileToDownload
     * @param storagePluginConf
     * @return
     * @throws ModuleException
     */
    @Transactional(readOnly = true)
    private InputStream downloadOnline(FileReference fileToDownload, StorageLocationConfiguration storagePluginConf)
            throws ModuleException {
        try {
            IOnlineStorageLocation plugin = pluginService
                    .getPlugin(storagePluginConf.getPluginConfiguration().getBusinessId());
            return plugin.retrieve(fileToDownload);
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException(String
                    .format("Unable to download file %s (checksum : %s) as its storage location %s is not active.",
                            fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                            fileToDownload.getLocation().toString()),
                    e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage(), e);
        }
    }

    /**
     * Try to download a nearline file. If the file is in the cache system, then the file can be download. Else,
     * a availability request is created and a {@link EntityNotFoundException} is thrown.
     * @param fileToDownload {@link FileReference} to download.
     * @return stream of the file from its cache copy.
     * @throws EntityNotFoundException If file is not in cache currently.
     */
    @Transactional(noRollbackFor = EntityNotFoundException.class)
    public InputStream download(FileReference fileToDownload)
            throws EntityNotFoundException, EntityOperationForbiddenException {
        Optional<CacheFile> ocf = cachedFileService.getCacheFile(fileToDownload.getMetaInfo().getChecksum());
        if (ocf.isPresent()) {
            // File is in cache and can be download
            try {
                // File is present in cache return stream
                return new FileInputStream(ocf.get().getLocation().getPath());
            } catch (FileNotFoundException e) {
                // Only log error and then ask for new availability of the file
                LOGGER.error(e.getMessage(), e);
            }
        }
        // ask for file availability and return a not available yet response
        fileCacheReqService.makeAvailable(Sets.newHashSet(fileToDownload), OffsetDateTime.now().plusHours(1),
                                          UUID.randomUUID().toString());
        throw new EntityOperationForbiddenException(String.format("File %s is not available yet. Please try later.",
                                                                  fileToDownload.getMetaInfo().getFileName()));
    }

    /**
     * Generate a public download URL for the file associated to the given Checksum
     * @param checksum
     * @return download url
     * @throws ModuleException if the Eureka server is not reachable
     */
    public String generateDownloadUrl(String checksum) throws ModuleException {
        Optional<ServiceInstance> instance = discoveryClient.getInstances(applicationName).stream().findFirst();
        if (instance.isPresent()) {
            String host = instance.get().getUri().toString();
            String path = Paths.get(FILES_PATH, DOWNLOAD_TOKEN_PATH).toString();
            String p = path.toString().replace("{checksum}", checksum);
            p = p.charAt(0) == '/' ? p.replaceFirst("/", "") : p;
            return String.format("%s/%s?scope=%s&%s=%s", host, p, tenantResolver.getTenant(), TOKEN_PARAM,
                                 createDownloadToken(checksum));
        } else {
            throw new ModuleException("Error getting storage microservice address from eureka client");
        }
    }

    /**
     * Check if given token is valid to download the file associated to the given checksum.
     * @param checksum
     * @param token
     */
    public boolean checkToken(String checksum, String token) {
        boolean accessGranted = downTokenRepo.existsByChecksumAndTokenAndExpirationDateAfter(checksum, token,
                                                                                             OffsetDateTime.now());
        if (!accessGranted) {
            LOGGER.info("Access denied to file {}. Token {} is no longer valid", checksum, token);
        }
        return accessGranted;
    }

    /**
     * Generate a download token for the file associated to the given checksum
     * @param checksum
     * @return download token
     */
    public String createDownloadToken(String checksum) {
        String newToken = UUID.randomUUID().toString();
        downTokenRepo.save(DownloadToken.build(newToken, checksum, OffsetDateTime.now().plusHours(1)));
        return newToken;
    }

    /**
     * Remove all expired download tokens
     */
    public void purgeTokens() {
        downTokenRepo.deleteByExpirationDateBefore(OffsetDateTime.now());
    }

}
