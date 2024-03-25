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
package fr.cnes.regards.modules.delivery.service.order.zip.steps;

import fr.cnes.regards.framework.s3.domain.StorageCommandResult;
import fr.cnes.regards.framework.s3.domain.StorageEntry;
import fr.cnes.regards.framework.s3.dto.StorageConfigDto;
import fr.cnes.regards.framework.s3.utils.StorageConfigUtils;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.order.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.s3.DeliveryS3ManagerService;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Upload a zip to a configured S3 remote location.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryZipUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryZipUploadService.class);

    private static final int LOCAL_TIMEOUT_MS = 60_000;

    private final DeliveryS3ManagerService s3ManagerService;

    public DeliveryZipUploadService(DeliveryS3ManagerService s3ManagerService) {
        this.s3ManagerService = s3ManagerService;
    }

    /**
     * Upload the zip previously created in the delivery workspace to a S3 remote location.
     *
     * @param deliveryRequest client request
     * @param localZipInfo    metadata about the local zip to upload
     * @return metadata about the zip uploaded
     * @throws DeliveryOrderException if the zip was not uploaded successfully. Could be for various reasons: the
     *                                local zip is not accessible, the S3 configured cannot be reached, the bucket
     *                                does not exist, etc.
     */
    public ZipDeliveryInfo uploadZipToS3DeliveryServer(DeliveryRequest deliveryRequest, ZipDeliveryInfo localZipInfo)
        throws DeliveryOrderException {
        String correlationId = deliveryRequest.getCorrelationId();
        LOGGER.debug("Starting uploading local delivery zip to remote S3 location (local zip info : '{}').",
                     localZipInfo);

        StorageConfigDto storageConfig = s3ManagerService.buildDeliveryStorageConfig(correlationId);
        ZipDeliveryInfo zipUploadedInfo = uploadZip(correlationId, storageConfig, localZipInfo);
        LOGGER.debug("Successfully uploaded zip on S3 delivery server (uploaded zip info '{}').", zipUploadedInfo);
        return zipUploadedInfo;
    }

    /**
     * Upload zip by using the {@link DeliveryS3ManagerService}. Throw exception according to the type of
     * {@link StorageCommandResult} returned.
     *
     * @param correlationId unique identifier to monitor the request
     * @param storageConfig S3 configuration built from the provided delivery settings
     * @param localZipInfo  zip to upload
     * @return metadata about the uploaded zip
     * @throws DeliveryOrderException if the zip was not uploaded successfully.
     */
    private ZipDeliveryInfo uploadZip(String correlationId,
                                      StorageConfigDto storageConfig,
                                      ZipDeliveryInfo localZipInfo) throws DeliveryOrderException {

        StorageEntry storageEntry = buildZipStorageEntry(storageConfig, localZipInfo);
        StorageCommandResult uploadedResult = s3ManagerService.uploadFileToDeliveryS3(correlationId,
                                                                                      storageConfig,
                                                                                      storageEntry,
                                                                                      localZipInfo.md5Checksum());
        if (uploadedResult instanceof StorageCommandResult.WriteSuccess resultSuccess) {
            return new ZipDeliveryInfo(correlationId,
                                       localZipInfo.name(),
                                       resultSuccess.getSize(),
                                       resultSuccess.getChecksum(),
                                       buildS3UploadedZipUri(storageEntry));
        } else {
            throw new DeliveryOrderException(String.format(
                "Could not upload zip to remote S3 delivery location '%s', got result of type '%s'. Check that S3"
                + " delivery is properly configured and that the zip to copy is accessible.",
                storageEntry.getFullPath(),
                uploadedResult.getClass().getSimpleName()));
        }
    }

    /**
     * Build the future zip S3 {@link StorageEntry} necessary for the S3 upload command.
     *
     * @param storageConfig S3 configuration
     * @param localZipInfo  zip to upload
     * @return {@link StorageEntry}
     */
    private StorageEntry buildZipStorageEntry(StorageConfigDto storageConfig, ZipDeliveryInfo localZipInfo) {
        Flux<ByteBuffer> buffers = DataBufferUtils.readInputStream(() -> DownloadUtils.getInputStreamThroughProxy(new URL(
                                                                       localZipInfo.uri()), Proxy.NO_PROXY, List.of(), LOCAL_TIMEOUT_MS, null),
                                                                   new DefaultDataBufferFactory(),
                                                                   DeliveryS3ManagerService.MULTIPART_THRESHOLD_BYTES)
                                                  .map(DataBuffer::asByteBuffer);

        String entryKey = StorageConfigUtils.entryKey(storageConfig, localZipInfo.name());

        return StorageEntry.builder()
                           .config(storageConfig)
                           .fullPath(entryKey)
                           .checksum(Option.some(Tuple.of("MD5", localZipInfo.md5Checksum())))
                           .size(Option.some(localZipInfo.sizeInBytes()))
                           .data(buffers)
                           .build();
    }

    /**
     * Build the uploaded zip uri from the {@link StorageEntry}.
     *
     * @param storageEntry metadata about the zip uploaded
     * @return zip uploaded uri s3://&lt;bucket&gt;/&lt;pathToZip&gt;/&lt;zipName&gt;.zip
     * @throws DeliveryOrderException if uri could not be built
     */
    private String buildS3UploadedZipUri(StorageEntry storageEntry) throws DeliveryOrderException {
        try {
            return new URI("s3",
                           storageEntry.getConfig().getBucket(),
                           "/" + storageEntry.getFullPath(),
                           null).toString();
        } catch (URISyntaxException e) {
            throw new DeliveryOrderException(String.format("Failed to get S3 uri from storage path '%s'",
                                                           storageEntry.getFullPath()), e);
        }
    }
}
