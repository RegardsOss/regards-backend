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
package fr.cnes.regards.modules.order.domain;

/**
 * @author Sébastien Binda
 */
public final class OrderControllerEndpointConfiguration {

    public static final String ORDERS_ROOT_MAPPING = "/orders";

    // FILES PATHS

    public static final String ORDERS_FILES_MAPPING = ORDERS_ROOT_MAPPING + "/files";

    public static final String ORDER_DATA_FILE_ID = "/{dataFileId}";

    public static final String ORDERS_FILES_DATA_FILE_ID = ORDERS_FILES_MAPPING + ORDER_DATA_FILE_ID;

    // PUBLIC FILES PATHS

    public static final String ORDERS_PUBLIC_FILES_MAPPING = ORDERS_ROOT_MAPPING + "/public/files";

    public static final String PUBLIC_ORDERS_FILES_DATA_FILE_ID = ORDERS_PUBLIC_FILES_MAPPING + ORDER_DATA_FILE_ID;

    //  DATASET PATHS

    public static final String ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES = ORDERS_ROOT_MAPPING
                                                                          + "/{orderId}/dataset"
                                                                          + "/{datasetId}/files";

    // AVAILABLE PATHS

    public static final String FIND_AVAILABLE_FILES_BY_ORDER_PATH = ORDERS_ROOT_MAPPING
                                                                    + "/{orderId}/files"
                                                                    + "/available";

    public static final String FIND_AVAILABLE_FILES_BY_SUBORDER_PATH = ORDERS_ROOT_MAPPING
                                                                       + "/{orderId}/filesTask"
                                                                       + "/{filesTaskId}/files/available";

    private OrderControllerEndpointConfiguration() {
        // class of constants
    }
}
