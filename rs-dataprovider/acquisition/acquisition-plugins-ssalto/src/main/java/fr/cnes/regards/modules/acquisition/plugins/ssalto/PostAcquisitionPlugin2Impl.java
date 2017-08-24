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

import ssalto.controlers.plugins.decl.IPostAcquisitionPlugin;
import ssalto.domain.data.SsaltoFile;

/**
 * Class PostAcquisitionPlugin2Impl
 * 
 * @author CS
 * @since 1.1
 */
public class PostAcquisitionPlugin2Impl implements IPostAcquisitionPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostAcquisitionPlugin2Impl.class);

    /**
     * Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.IPostAcquisitionPlugin#runPlugin(java.util.List, java.lang.String)
     * @since 1.1
     */
    @Override
    public void runPlugin(List<SsaltoFile> pAcquiredFiles, String pDescriptorFileDir) {

        // Fake method
        // Just log an info message
        LOGGER.info("Execution of post acquisition plugin 2 OK");
    }

}
