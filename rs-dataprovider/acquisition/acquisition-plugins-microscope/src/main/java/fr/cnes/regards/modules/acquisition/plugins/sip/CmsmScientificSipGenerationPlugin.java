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
package fr.cnes.regards.modules.acquisition.plugins.sip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import static fr.cnes.regards.modules.acquisition.plugins.Microscope.*;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import ucar.nc2.NetcdfFile;

/**
 * CMSM scientific product SIP generation plugin</br>
 * @author Olivier Rousselot
 */
@Plugin(id = "CmsmScientificSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "Generate SIP from CMSM scientific product ZIP archive file", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class CmsmScientificSipGenerationPlugin implements ISipGenerationPlugin {

    @Override
    public SIP generate(Product product) throws ModuleException {
        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        AcquisitionFile af = product.getActiveAcquisitionFiles().get(0);
        try {
            sipBuilder.getContentInformationBuilder()
                    .setDataObject(DataType.RAWDATA, af.getFilePath().toAbsolutePath(), af.getChecksumAlgorithm(),
                                   af.getChecksum());
            sipBuilder.getContentInformationBuilder().setSyntax(Microscope.GZIP_MIME_TYPE);
            sipBuilder.addContentInformation();

            // Add creation event
            sipBuilder.addEvent("Product SIP generation");
            ZipFile zipFile = new ZipFile(af.getFilePath().toFile());
            Optional<? extends ZipEntry> ncdFileEntryOpt = zipFile.stream()
                    .filter(entry -> entry.getName().endsWith(Microscope.NCD_EXT)).findFirst();
            if (!ncdFileEntryOpt.isPresent()) {
                throw new ModuleException(
                        String.format("ZIP archive file '%s' doesn't contain a NCD file (with '%s' extension)",
                                      af.getFilePath().toString(), Microscope.NCD_EXT));
            }
            // UNZIP ncd file
            File ncdTmpFile = File.createTempFile(product.getProductName(), Microscope.NCD_EXT);
            ncdTmpFile.deleteOnExit();
            try (InputStream ncdZipInputStream = zipFile.getInputStream(ncdFileEntryOpt.get())) {
                // REPLACE_EXISTING because File.createTempFile has already creted the file
                Files.copy(ncdZipInputStream, ncdTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            // EXTRACT values from ncd file
            NetcdfFile netcdfFile = NetcdfFile.open(ncdTmpFile.getAbsolutePath());
            sipBuilder.addDescriptiveInformation(Microscope.SESSION_NB, findValue(netcdfFile, SESSION_NB_NC_TAG));
            sipBuilder.addDescriptiveInformation(START_DATE, findValue(netcdfFile, START_DATE_NC_TAG));
            sipBuilder.addDescriptiveInformation(END_DATE, findValue(netcdfFile, END_DATE_NC_TAG));
            sipBuilder.addDescriptiveInformation(PHASE, findValue(netcdfFile, PHASE_NC_TAG));
            sipBuilder.addDescriptiveInformation(TECHNO, findValue(netcdfFile, TECHNO_NC_TAG));
            sipBuilder.addDescriptiveInformation(SESSION_TYPE, findValue(netcdfFile, SESSION_TYPE_NC_TAG));
            sipBuilder.addDescriptiveInformation(SESSION_SUB_TYPE, findValue(netcdfFile, SESSION_SUB_TYPE_NC_TAG));
            sipBuilder.addDescriptiveInformation(SPIN, findValue(netcdfFile, ROTATE_MOD_NC_TAG));
            sipBuilder.addDescriptiveInformation(CAL_PARAM, findValue(netcdfFile, CAL_PARAM_NC_TAG));
            sipBuilder.addDescriptiveInformation(RECORD_FILE_NAME, findValue(netcdfFile, RECORD_FILE_NAME_NC_TAG));
            sipBuilder.addDescriptiveInformation(RECORD_VERSION, findValue(netcdfFile, RECORD_VERSION_NC_TAG));
            sipBuilder.addDescriptiveInformation(ENV_CONSTRAINT, findValue(netcdfFile, ENV_CONSTRAINT_NC_TAG));
            sipBuilder.addDescriptiveInformation(ACTIVE_SU, findValue(netcdfFile, ACTIVE_SU_NC_TAG));
            sipBuilder.addDescriptiveInformation(PID_VERSION, findValue(netcdfFile, PID_VERSION_NC_TAG));
            sipBuilder.addDescriptiveInformation(ORBITS_COUNT, findValue(netcdfFile, ORBITS_COUNT_NC_TAG));
            sipBuilder.addDescriptiveInformation(COMMENT, findValue(netcdfFile, COMMENT_NC_TAG));
            netcdfFile.close();
        } catch (IOException e) {
            throw new ModuleException("I/O error occured", e);
        }
        return sipBuilder.build();
    }

    private static Object findValue(NetcdfFile netcdfFile, String attName) {
        return netcdfFile.findGlobalAttributeIgnoreCase(attName).getValue(0);
    }
}
