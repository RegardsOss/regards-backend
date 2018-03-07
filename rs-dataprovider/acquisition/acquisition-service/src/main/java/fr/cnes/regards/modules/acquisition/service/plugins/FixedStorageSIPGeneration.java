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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * SIP generation with configurable storage information write in miscInformation section.
 *
 * @author Marc Sordi
 */
@Plugin(id = "FixedStorageSIPGeneration", version = "1.0.0-SNAPSHOT",
        description = "Generate SIP by adding storage information configured in the miscInformation.storage section",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class FixedStorageSIPGeneration extends AbstractMiscStorageInformation implements ISipGenerationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedStorageSIPGeneration.class);

    @Override
    public SIP generate(Product product) throws ModuleException {

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Fill SIP with product information
        for (AcquisitionFile af : product.getActiveAcquisitionFiles()) {
            try {
                sipBuilder.getContentInformationBuilder().setDataObject(af.getFileInfo().getDataType(),
                                                                        af.getFilePath().toAbsolutePath().toUri()
                                                                                .toURL(),
                                                                        af.getChecksumAlgorithm(), af.getChecksum());
                sipBuilder.getContentInformationBuilder().setSyntax(af.getFileInfo().getMimeType());
                sipBuilder.addContentInformation();
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new EntityInvalidException(e.getMessage());
            }
        }

        // Add optional storage information into misc section
        addMiscStorageInfomation(sipBuilder);

        // Add creation event
        sipBuilder.addEvent("Product SIP generation with fixed storage in misc section");

        return sipBuilder.build();
    }

}
