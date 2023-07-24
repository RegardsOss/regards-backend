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
package fr.cnes.regards.modules.order.client.feign;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.order.domain.OrderControllerEndpointConfiguration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * REST Client to download files
 *
 * @author Iliana Ghazali
 */
@RestClient(name = "rs-order", contextId = "rs-order.order.data.file.client")
public interface IOrderDataFileClient {

    @GetMapping(path = OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID, produces = MediaType.ALL_VALUE)
    ResponseEntity<InputStreamResource> downloadFile(@PathVariable("dataFileId") Long dataFileId);
}
