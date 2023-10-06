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
package fr.cnes.regards.modules.delivery.service.config.mock;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileClient;
import org.springframework.http.HttpStatus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashMap;

import static fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils.TEST_FILES_ORDER_RESOURCES;

/**
 * Bean to mock rs-order {@link IOrderDataFileClient} behaviour.
 *
 * @author Iliana Ghazali
 **/
public class OrderDataFileClientMock implements IOrderDataFileClient {

    @Override
    public Response downloadFile(Long dataFileId) {
        try {
            return Response.builder()
                           .status(HttpStatus.OK.value())
                           .request(Request.create(Request.HttpMethod.GET,
                                                   "url",
                                                   new HashMap<>(),
                                                   Request.Body.empty(),
                                                   new RequestTemplate()))
                           .body(new FileInputStream(Path.of(String.format(TEST_FILES_ORDER_RESOURCES.resolve("data-%d")
                                                                                                     .resolve(
                                                                                                         "file-%d.txt")
                                                                                                     .toString(),
                                                                           dataFileId,
                                                                           dataFileId)).toFile()), 100)
                           .build();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
