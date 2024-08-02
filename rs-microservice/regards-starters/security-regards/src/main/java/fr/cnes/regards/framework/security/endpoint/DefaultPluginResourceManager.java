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
package fr.cnes.regards.framework.security.endpoint;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class DefaultPluginResourceManager
 * <p>
 * Default implementation for Plugin resource endpoints management
 *
 * @author CS
 */
public class DefaultPluginResourceManager implements IPluginResourceManager {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPluginResourceManager.class);

    @Override
    public List<ResourceMapping> manageMethodResource(ResourceMapping pResourceMapping) {
        final List<ResourceMapping> mappings = new ArrayList<>();
        LOG.warn("There is no implementation fo plugin endpoints resource management");
        return mappings;
    }

}
