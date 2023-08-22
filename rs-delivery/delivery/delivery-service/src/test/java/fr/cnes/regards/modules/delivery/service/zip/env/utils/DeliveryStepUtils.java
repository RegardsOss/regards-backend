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
package fr.cnes.regards.modules.delivery.service.zip.env.utils;

import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryStatus;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.order.domain.dto.OrderDataFileDTO;
import org.apache.commons.collections4.ListUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Iliana Ghazali
 **/
public class DeliveryStepUtils {

    public static final String DELIVERY_CORRELATION_ID = "corrId-test";

    // RESOURCES

    public static final Path WORKSPACE_PATH = Path.of("target/workspace");

    public static final Path TEST_RESOURCES = Path.of("src/test/resources/order");

    public static final Path TEST_MULTIPLE_ZIP_ORDER_RESOURCE = TEST_RESOURCES.resolve("multiple-zip");

    public static final Path TEST_SINGLE_ZIP_ORDER_RESOURCE = TEST_RESOURCES.resolve("single-zip");

    public static final Path TEST_FILES_ORDER_RESOURCES = TEST_RESOURCES.resolve("files");

    // DELIVERY PATTERNS

    public static final String PRODUCT_FOLDER_PATTERN = "%s_%d"; // <productId>_<version>

    public static final String MULTIPLE_FILES_ZIP_NAME_PATTERN = "delivery-%s.zip"; // delivery-<corrId>.zip

    public static DeliveryRequest buildDeliveryRequest() {
        return new DeliveryRequest(DELIVERY_CORRELATION_ID,
                                   "regards-delivery@test.fr",
                                   new DeliveryStatus(OffsetDateTime.now(),
                                                      OffsetDateTime.now(),
                                                      2,
                                                      DeliveryRequestStatus.GRANTED,
                                                      null,
                                                      null),
                                   95L,
                                   1,
                                   "rs-delivery",
                                   1);
    }

    public static PagedModel<EntityModel<OrderDataFileDTO>> handleOrderDataFilesDtosByPage(int pageNumber,
                                                                                           int pageSize,
                                                                                           List<OrderDataFileDTO> orderDataFileDtos) {
        List<List<EntityModel<OrderDataFileDTO>>> orderDtoSets = ListUtils.partition(orderDataFileDtos.stream()
                                                                                                      .map(EntityModel::of)
                                                                                                      .toList(),
                                                                                     pageSize);
        int totalPages = orderDtoSets.size();
        List<Link> links = new ArrayList<>();
        if (pageNumber != totalPages - 1) {
            links.add(Link.of("sample", "next"));
        }
        return PagedModel.of(orderDtoSets.get(pageNumber),
                             new PagedModel.PageMetadata(pageSize, pageNumber, orderDataFileDtos.size(), totalPages),
                             links);

    }

    public static List<OrderDataFileDTO> buildOrderDataFileDtos() {
        return IntStream.range(0, 3).mapToObj(DeliveryStepUtils::buildOrderDataFile).toList();
    }

    public static OrderDataFileDTO buildOrderDataFile(int id) {
        String filename = String.format("file-%d.txt", id);
        Path filePath = Path.of(String.format(TEST_FILES_ORDER_RESOURCES.resolve("data-%d")
                                                                        .resolve(filename)
                                                                        .toString(), id, id));
        try {
            return new OrderDataFileDTO((long) id,
                                        String.format("product-%d", id),
                                        1,
                                        filePath.toUri().toString(),
                                        MediaType.TEXT_PLAIN,
                                        ChecksumUtils.computeHexChecksum(filePath, "MD5"),
                                        filePath.toFile().length(),
                                        filename);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
