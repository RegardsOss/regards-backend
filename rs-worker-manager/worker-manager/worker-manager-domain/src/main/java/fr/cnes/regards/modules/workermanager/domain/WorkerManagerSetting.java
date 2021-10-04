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
package fr.cnes.regards.modules.workermanager.domain;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dynamic tenant settings for WorkerManager microservice
 */
public class WorkerManagerSetting {

    public static final String SKIP_CONTENT_TYPES_NAME = "skipContentTypes";

    public static final DynamicTenantSetting SKIP_CONTENT_TYPES = new DynamicTenantSetting(SKIP_CONTENT_TYPES_NAME,
                                                                                           "List of content-types to ignore for requests dispatch.",
                                                                                           new ArrayList<String>());

    public static final List<DynamicTenantSetting> SETTING_LIST = Arrays.asList(SKIP_CONTENT_TYPES);

    private WorkerManagerSetting() {
    }
}
