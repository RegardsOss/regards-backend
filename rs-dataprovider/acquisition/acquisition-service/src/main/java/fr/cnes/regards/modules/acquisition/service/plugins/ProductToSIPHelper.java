/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;

/**
 *
 * Help to fill the {@link SIP} required field according to the related {@link Product}
 *
 * @author Marc Sordi
 *
 */
public final class ProductToSIPHelper {

    private ProductToSIPHelper() {
        // Nothing to do
    }

    public static SIPBuilder initFrom(Product product) throws ModuleException {

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Fill SIP with product information
        for (AcquisitionFile af : product.getAcquisitionFiles()) {
            String checksum;
            try {
                checksum = ChecksumUtils.computeHexChecksum(af.getFilePath(),
                                                            AcquisitionProcessingChain.CHECKSUM_ALGORITHM);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new ModuleException(String.format("Error calculating file checksum. Cause %s", e.getMessage()), e);
            }
            sipBuilder.getContentInformationBuilder()
                    .setDataObject(af.getFileInfo().getDataType(), af.getFilePath().toAbsolutePath(),
                                   AcquisitionProcessingChain.CHECKSUM_ALGORITHM, checksum);
            sipBuilder.getContentInformationBuilder().setSyntax(af.getFileInfo().getMimeType());
            sipBuilder.addContentInformation();
        }
        return sipBuilder;
    }
}
