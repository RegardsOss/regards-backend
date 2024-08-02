/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.manager;

/**
 * Delivery jobs priority management. The higher the number, the more important the job is.
 *
 * @author Iliana Ghazali
 */
public final class DeliveryJobPriority {

    public static final int ORDER_DELIVERY_ZIP_JOB_PRIORITY = 0;

    public static final int CLEAN_ORDER_JOB_PRIORITY = 100;

    private DeliveryJobPriority() {
        // constants class
    }

}
