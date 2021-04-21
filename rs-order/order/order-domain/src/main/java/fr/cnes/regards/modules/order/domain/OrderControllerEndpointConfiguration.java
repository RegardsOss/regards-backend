/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.domain;

/**
 *
 * @author Sébastien Binda
 *
 */
public class OrderControllerEndpointConfiguration {

    public static final String ORDERS_FILES_MAPPING = "/orders/files";

    public static final String ORDERS_PUBLIC_FILES_MAPPING = "/orders/public/files";

    public static final String ORDER_DATA_FILE_ID = "/{dataFileId}";

    public static final String ORDERS_FILES_DATA_FILE_ID = ORDERS_FILES_MAPPING + ORDER_DATA_FILE_ID;

    public static final String PUBLIC_ORDERS_FILES_DATA_FILE_ID = ORDERS_PUBLIC_FILES_MAPPING + ORDER_DATA_FILE_ID;

    public static final String ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES = "/orders/{orderId}/dataset/{datasetId}/files";

}
