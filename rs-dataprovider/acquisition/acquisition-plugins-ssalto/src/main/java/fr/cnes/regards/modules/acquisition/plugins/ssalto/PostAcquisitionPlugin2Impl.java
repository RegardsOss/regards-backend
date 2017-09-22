/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSIP;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class PostAcquisitionPlugin2Impl implements IPostProcessSIP {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostAcquisitionPlugin2Impl.class);

    @Override
    public void runPlugin(List<AcquisitionFile> acquiredFiles, String sipDirectory) throws ModuleException {
        LOGGER.info("Execution of post acquisition plugin 2 OK");

    }

}
