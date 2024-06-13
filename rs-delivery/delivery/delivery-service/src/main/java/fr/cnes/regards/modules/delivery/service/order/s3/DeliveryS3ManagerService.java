/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.s3;

import fr.cnes.regards.framework.s3.client.S3HighLevelReactiveClient;
import fr.cnes.regards.framework.s3.domain.*;
import fr.cnes.regards.framework.s3.dto.StorageConfigDto;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import fr.cnes.regards.modules.delivery.domain.settings.S3DeliveryServer;
import fr.cnes.regards.modules.delivery.service.settings.DeliverySettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Delivery utility methods to manage interactions with a S3 server.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryS3ManagerService {

    public static final int MULTIPART_THRESHOLD_BYTES = 5_242_880;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryS3ManagerService.class);

    private final DeliverySettingService settingService;

    public DeliveryS3ManagerService(DeliverySettingService settingService) {
        this.settingService = settingService;
    }

    private S3HighLevelReactiveClient initS3Client() {
        Scheduler scheduler = Schedulers.newParallel("delivery-s3-client", 10);
        return new S3HighLevelReactiveClient(scheduler, MULTIPART_THRESHOLD_BYTES, 10);
    }

    /**
     * Build a S3 {@link StorageConfigDto} from the delivery settings.
     *
     * @param deliveryRoot unique identifier to build the parent delivery location
     * @return {@link StorageConfigDto}
     * @throws DeliveryOrderException if S3 access uri cannot be built
     */
    public StorageConfigDto buildDeliveryStorageConfig(String deliveryRoot) throws DeliveryOrderException {
        S3DeliveryServer s3Config = settingService.getValue(DeliverySettings.S3_SERVER);
        String s3DeliveryBucket = settingService.getValue(DeliverySettings.DELIVERY_BUCKET);

        try {
            return new StorageConfigBuilder(new URI(s3Config.getScheme(),
                                                    null,
                                                    s3Config.getHost(),
                                                    s3Config.getPort(),
                                                    null,
                                                    null,
                                                    null).toString(),
                                            s3Config.getRegion(),
                                            s3Config.getKey(),
                                            s3Config.getSecret()).rootPath(deliveryRoot)
                                                                 .bucket(s3DeliveryBucket)
                                                                 .build();
        } catch (URISyntaxException e) {
            throw new DeliveryOrderException("Could not get S3 endpoint from configuration, check if "
                                             + "the delivery settings were properly set.", e);
        }
    }

    /**
     * Upload a file to the S3 server in the configured location. A checksum verification if done making sure the
     * file was successfully uploaded.
     *
     * @param correlationId unique identifier to monitor the process
     * @param storageConfig S3 configuration
     * @param storageEntry  metadata about the file to upload
     * @param fileChecksum  md5 file checksum
     * @return {@link StorageCommandResult} indicating the status of the upload
     */
    public StorageCommandResult uploadFileToDeliveryS3(String correlationId,
                                                       StorageConfigDto storageConfig,
                                                       StorageEntry storageEntry,
                                                       String fileChecksum) {
        // Define command to upload file
        StorageCommandID cmdId = new StorageCommandID(String.format("upload-delivery-%s", correlationId),
                                                      UUID.randomUUID());
        StorageCommand.Write writeCmd = new StorageCommand.Write.Impl(storageConfig,
                                                                      cmdId,
                                                                      storageEntry.getFullPath(),
                                                                      storageEntry,
                                                                      fileChecksum);
        // Upload file
        try (S3HighLevelReactiveClient s3Client = initS3Client()) {
            return s3Client.write(writeCmd)
                           .doOnSuccess(result -> LOGGER.info(
                               "Executed write command file to remote S3 delivery location with result " + "type '{}'.",
                               result.getClass().getSimpleName()))
                           .doOnError(t -> LOGGER.error("Unexpected error, failed to write file on S3 remote location "
                                                        + "'{}'.", storageEntry.getFullPath(), t))
                           .block();
        }
    }

}
