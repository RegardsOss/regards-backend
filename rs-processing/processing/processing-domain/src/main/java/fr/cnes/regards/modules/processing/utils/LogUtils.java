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
package fr.cnes.regards.modules.processing.utils;

import fr.cnes.regards.framework.module.log.CorrelationIdUtils;

/**
 * @author Théo Lasserre
 */
public class LogUtils {

    public static final String ORDER_ID_LOG_KEY = "ORDER_ID=";

    public static void setOrderIdInMdc(String batchCorrelationId) {
        // See BatchSuborderCorrelationIdentifier to understand batchCorrelationId pattern
        int orderIdFirstChar = batchCorrelationId.indexOf("-") + 1;
        int orderIdLastChar = batchCorrelationId.indexOf("_");

        // Set log correlation id
        CorrelationIdUtils.setCorrelationId(
            ORDER_ID_LOG_KEY + batchCorrelationId.substring(orderIdFirstChar, orderIdLastChar));
    }
}
