/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Test plugin class impl
 * @author Sébastien Binda
 */
@Plugin(description = "Plugin test", id = UniqueConfActivePluginImpl.PLUGIN_ID, version = "0.0.1",
        author = "REGARDS Dream Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class UniqueConfActivePluginImpl implements IUniqueConfActivePlugin {

    public static final String PLUGIN_ID = "UniqueConfActivePluginImpl";

}
