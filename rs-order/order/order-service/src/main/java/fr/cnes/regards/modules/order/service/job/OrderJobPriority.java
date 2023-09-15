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
package fr.cnes.regards.modules.order.service.job;

/**
 * Order jobs priority management
 *
 * @author Stephane Cortine
 */
public final class OrderJobPriority {

    public static final int CREATE_ORDER_JOB_PRIORITY = 0;

    public static final int PROCESS_ORDER_MIN_JOB_PRIORITY = 80;

    public static final int PROCESS_ORDER_MAX_JOB_PRIORITY = 100;

    public static final int CANCEL_ORDER_JOB_PRIORITY = 5000;

    private OrderJobPriority() {
    }
}
