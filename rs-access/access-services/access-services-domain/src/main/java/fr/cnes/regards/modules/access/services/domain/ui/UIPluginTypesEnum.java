/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.domain.ui;

/**
 *
 * Class PluginTypesEnum
 *
 * Enum for all plugin types.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public enum UIPluginTypesEnum {

    /**
     * UI Plugins for search-forms. Create a new UI Criteria.
     */
    CRITERIA,

    /**
     * UI Plugins for search results. To apply a new service to a selected list of entities.
     */
    SERVICE;

}
