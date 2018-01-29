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
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 *
 * Help to fill the {@link SIP} required field according to the related {@link Product}
 *
 * @author Marc Sordi
 *
 */
public class ProductToSIPHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductToSIPHelper.class);

    private ProductToSIPHelper() {
        // Nothing to do
    }

    public static SIPBuilder initFrom(Product product) throws ModuleException {

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Fill SIP with product information
        for (AcquisitionFile af : product.getAcquisitionFiles()) {
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
        return sipBuilder;
    }
}
