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
package fr.cnes.regards.modules.acquisition.service.plugins.cdpp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * @author Marc Sordi
 *
 */
public class Arcad3IsoprobeDensiteProductPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Arcad3IsoprobeDensiteProductPluginTest.class);

    private static final Path resource = Paths.get("src", "test", "resources", "DA_TC_ARC_ISO_DENS");

    private static final String PRODUCT_NAME = "ISO_DENS_19810930_1422";

    @Test
    public void testProductPlugin() throws ModuleException {

        IProductPlugin plugin = new Arcad3IsoprobeDensiteProductPlugin();

        // Raw data
        Path rawData = resource.resolve("ISO_DENS_19810930_1422");
        String rawDataProductName = plugin.getProductName(rawData);

        // Quicklook SD
        Path sd = resource.resolve("iso_nete_19810930_1422B.png");
        String SDProductName = plugin.getProductName(sd);
        Assert.assertEquals(rawDataProductName, SDProductName);

        // Quicklook MD
        Path md = resource.resolve("iso_nete_19810930_1422C.png");
        String MDProductName = plugin.getProductName(md);
        Assert.assertEquals(rawDataProductName, MDProductName);
    }

    @Test
    public void extractTimePeriod() throws IOException {

        String linePattern = ".*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z).*";
        Pattern pattern = Pattern.compile(linePattern);

        // Raw data
        Path rawData = resource.resolve(PRODUCT_NAME);
        List<String> lines = Files.readAllLines(rawData);

        LOGGER.info("First line : " + lines.get(0));

        // Extract start date from first line
        Matcher m = pattern.matcher(lines.get(0));
        if (m.matches()) {
            LOGGER.info("START_DATE : " + m.group(1));
        }

        // Extract stop date from last line
        m = pattern.matcher(lines.get(lines.size() - 1));
        if (m.matches()) {
            LOGGER.info("STOP_DATE : " + m.group(1));
        }
    }

    @Test
    public void generateSIP() throws ModuleException {

        Product product = new Product();
        product.setProductName(PRODUCT_NAME);

        // Raw data
        AcquisitionFileInfo afi = new AcquisitionFileInfo();
        afi.setDataType(DataType.RAWDATA);
        afi.setMimeType(MimeType.valueOf("text/plain"));

        AcquisitionFile af = new AcquisitionFile();
        af.setFilePath(resource.resolve(PRODUCT_NAME));
        af.setChecksum("rawdatachecksum");
        af.setChecksumAlgorithm("MD5");
        af.setFileInfo(afi);
        af.setState(AcquisitionFileState.ACQUIRED);

        product.addAcquisitionFile(af);

        // Quicklook SD
        afi = new AcquisitionFileInfo();
        afi.setDataType(DataType.QUICKLOOK_SD);
        afi.setMimeType(MimeType.valueOf("image/png"));

        af = new AcquisitionFile();
        af.setFilePath(resource.resolve("iso_nete_19810930_1422B.png"));
        af.setChecksum("rawdatachecksum");
        af.setChecksumAlgorithm("MD5");
        af.setFileInfo(afi);
        af.setState(AcquisitionFileState.ACQUIRED);

        product.addAcquisitionFile(af);

        // Quicklook MD
        afi = new AcquisitionFileInfo();
        afi.setDataType(DataType.QUICKLOOK_SD);
        afi.setMimeType(MimeType.valueOf("image/png"));

        af = new AcquisitionFile();
        af.setFilePath(resource.resolve("iso_nete_19810930_1422C.png"));
        af.setChecksum("rawdatachecksum");
        af.setChecksumAlgorithm("MD5");
        af.setFileInfo(afi);
        af.setState(AcquisitionFileState.ACQUIRED);

        product.addAcquisitionFile(af);

        // Plugin
        ISipGenerationPlugin plugin = new Arcad3IsoprobeDensiteSIPGenerationPlugin();
        SIP sip = plugin.generate(product);
        Assert.assertNotNull(sip);
    }
}
