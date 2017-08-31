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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.CalculusTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.CompressionTypeEnum;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class MultipleFileNameFinderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileNameFinderTest.class);

    private static final String FINDER_PATH = "src/test/resources/income/data/finder/";

    private final String filenamePattern = "R_TCLOG_JA2_(\\p{Alnum}*)_(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})_(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})";

    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";

    private final String tclogZipFileName = "R_TCLOG_JA2_T2L2MC_2008_12_26_10_28_22.zip";

    @Test
    public void getValueListEmptyList() throws PluginAcquisitionException {
        Assert.assertTrue(new File(FINDER_PATH + tclogZipFileName).getAbsolutePath(),
                          new File(FINDER_PATH + tclogZipFileName).exists());
        FileNameFinder fileNameFinder = new FileNameFinder();
        fileNameFinder.setAttributProperties(initConfProperties());
        Map<File, ?> fileMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) fileNameFinder.getValueList(fileMap, null);
        Assert.assertTrue(resultList.isEmpty());
    }

    @Test
    public void getValueListZippedOptionValue() throws Exception {
        AcquisitionFile testFile = initAcquisitionFile(tclogZipFileName);

        Map<File, File> fileMap = new HashMap<>();
        fileMap.put(new File(testFile.getAcquisitionInformations().getAcquisitionDirectory(), testFile.getFileName()),
                    null);

        MultipleFileNameFinder fileNameFinder = new MultipleFileNameFinder();
        fileNameFinder.setFileInZipNamePattern(filenamePattern);
        fileNameFinder.setValueType(AttributeTypeEnum.TYPE_STRING.toString());
        fileNameFinder.setAttributProperties(initConfProperties());
        fileNameFinder.setUnzipBefore(Boolean.toString(true));
        fileNameFinder.setCompression(CompressionTypeEnum.ZIP.toString());

        fileNameFinder.addGroupNumber("1");

        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) fileNameFinder.getValueList(fileMap, null);
        Assert.assertEquals(2, resultList.size());
        Assert.assertTrue(resultList.contains("CHRIS"));
        Assert.assertTrue(resultList.contains("T2L2"));
    }

    @Test
    public void getValueListZippedStartDateValue() throws Exception {
        AcquisitionFile testFile = initAcquisitionFile(tclogZipFileName);

        Map<File, ?> fileMap = new HashMap<>();
        fileMap.put(new File(testFile.getAcquisitionInformations().getAcquisitionDirectory(), testFile.getFileName()),
                    null);

        MultipleFileNameFinder fileNameFinder = new MultipleFileNameFinder();
        fileNameFinder.setFormatRead(DATE_FORMAT);
        fileNameFinder.setValueType(AttributeTypeEnum.TYPE_DATE_TIME.toString());
        fileNameFinder.setFileInZipNamePattern(filenamePattern);
        fileNameFinder.setAttributProperties(initConfProperties());
        fileNameFinder.setUnzipBefore("true");
        fileNameFinder.setCompression(CompressionTypeEnum.ZIP.toString());

        fileNameFinder.addGroupNumber("2");
        fileNameFinder.setCalculus(CalculusTypeEnum.MIN.toString());

        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) fileNameFinder.getValueList(fileMap, null);
        Assert.assertEquals(1, resultList.size());

        LocalDateTime expectedLdt = LocalDateTime.of(2007, 12, 26, 8, 26, 26);

        Assert.assertEquals(Date.from(expectedLdt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
                            ((Date) resultList.get(0)).getTime());
    }

    @Test
    public void getValueListZippedStopDateValue() throws Exception {
        AcquisitionFile testFile = initAcquisitionFile(tclogZipFileName);

        Map<File, ?> fileMap = new HashMap<>();
        fileMap.put(new File(testFile.getAcquisitionInformations().getAcquisitionDirectory(), testFile.getFileName()),
                    null);

        MultipleFileNameFinder fileNameFinder = new MultipleFileNameFinder();
        fileNameFinder.setFormatRead(DATE_FORMAT);
        fileNameFinder.setValueType(AttributeTypeEnum.TYPE_DATE_TIME.toString());
        fileNameFinder.setFileInZipNamePattern(filenamePattern);
        fileNameFinder.setAttributProperties(initConfProperties());
        fileNameFinder.setUnzipBefore("true");
        fileNameFinder.setCompression(CompressionTypeEnum.ZIP.toString());

        fileNameFinder.addGroupNumber("3");
        fileNameFinder.setCalculus(CalculusTypeEnum.MAX.toString());

        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) fileNameFinder.getValueList(fileMap, null);
        Assert.assertEquals(1, resultList.size());

        LocalDateTime expectedLdt = LocalDateTime.of(2007, 12, 26, 10, 18, 13);

        Assert.assertEquals(Date.from(expectedLdt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
                            ((Date) resultList.get(0)).getTime());
    }

    @After
    public void clean() throws IOException {
        Path rootPath = Paths.get(FINDER_PATH);
        Files.walk(rootPath).filter(p -> !p.toFile().getName().endsWith(rootPath.getFileName().toString())) // do not delete the root path
                .filter(p -> !p.toFile().getName().equals(tclogZipFileName)).sorted(Comparator.reverseOrder())
                .map(Path::toFile).peek(f -> LOGGER.debug(f.getName())).forEach(File::delete);
    }

    private AcquisitionFile initAcquisitionFile(String pFileName) {
        AcquisitionFile acquisitionFile = new AcquisitionFile();

        FileAcquisitionInformations acquisitionInformations = new FileAcquisitionInformations();
        acquisitionInformations.setAcquisitionDirectory(FINDER_PATH);
        acquisitionFile.setAcquisitionInformations(acquisitionInformations);

        acquisitionFile.setFileName(pFileName);

        return acquisitionFile;
    }

    private PluginConfigurationProperties initConfProperties() {
        PluginConfigurationProperties_mock mockProperties = new PluginConfigurationProperties_mock();
        mockProperties.setFileNamePattern(filenamePattern);
        return mockProperties;
    }
}
