/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.test.report.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.RequirementMatrixReportListener;
import fr.cnes.regards.framework.test.report.exception.ReportException;

/**
 * Help to read and write XML based on JAXB JAVA annotation.
 * @author msordi
 */
public final class XmlHelper {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(XmlHelper.class);

    public static final String MISSING_JAXB_ANNOTATED_CLASS = "Missing JAXB annotated class";

    private XmlHelper() {
    }

    /**
     * Write data to a file
     * @param <T> JAXBElement
     * @param pDirectory file directory (created if don't exist)
     * @param pFilename filename
     * @param pClass type of JAXB element to write
     * @param pJaxbElement JAXB element
     * @throws ReportException if report cannot be write
     */
    public static <T> void write(Path pDirectory, String pFilename, Class<T> pClass, T pJaxbElement)
            throws ReportException {

        // Validate
        assertNotNull(pDirectory, "Missing directory path");
        assertNotNull(pFilename, "Missing filename");
        assertNotNull(pClass, MISSING_JAXB_ANNOTATED_CLASS);
        assertNotNull(pJaxbElement, "No element to write");

        // Create directory
        if (!pDirectory.toFile().exists()) {
            pDirectory.toFile().mkdirs();
        }

        final File targetFile = pDirectory.resolve(pFilename).toFile();

        try {
            // Init marshaller
            final JAXBContext jaxbContext = JAXBContext.newInstance(pClass);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // Format output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Marshall data
            jaxbMarshaller.marshal(pJaxbElement, targetFile);
        } catch (JAXBException e) {
            final String message = "Error while marshalling data";
            LOG.error(message, e);
            throw new ReportException(message);
        }

    }

    /**
     * Read data from file
     * @param <T> JAXB annotated class
     * @param pDirectory file directory
     * @param pFilename filename
     * @param pClass type of JAXB element to read
     * @return JAXB element
     * @throws ReportException if report cannot be read
     */
    public static <T> T read(Path pDirectory, String pFilename, Class<T> pClass)
            throws ReportException {
        // Validate
        assertNotNull(pDirectory, "Missing directory path");
        assertNotNull(pFilename, "Missing filename");
        assertNotNull(pClass, MISSING_JAXB_ANNOTATED_CLASS);

        //because jaxb unmarshall uses urls, lets url encode the %
        final File sourceFile = Paths
                .get(pDirectory.resolve(pFilename).toAbsolutePath().toString().replaceAll("%", "%25")).toFile();

        try {
            // Init unmarshaller
            final JAXBContext jaxbContext = JAXBContext.newInstance(pClass);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            // Unmarshall data
            @SuppressWarnings("unchecked") final T results = (T) jaxbUnmarshaller.unmarshal(sourceFile);

            return results;
        } catch (JAXBException e) {
            final String message = "Error while marshalling data";
            LOG.error(message, e);
            throw new ReportException(message);
        }

    }

    /**
     * Read data from file
     * @param <T> JAXB annotated class
     * @param pFilePath full file path
     * @param pClass type of JAXB element to read
     * @return JAXB element
     * @throws ReportException if report cannot be read
     */
    public static <T> T read(Path pFilePath, Class<T> pClass) throws ReportException, UnsupportedEncodingException {
        // Validate
        assertNotNull(pFilePath, "Missing full file path");
        assertNotNull(pClass, MISSING_JAXB_ANNOTATED_CLASS);
        return read(pFilePath.getParent(), pFilePath.getFileName().toString(), pClass);
    }

    /**
     * Aggregate all reports found in base path in a single one
     * @param pBasePath base directory
     * @return {@link XmlRequirements}
     * @throws ReportException if method cannot aggregate reports
     */
    public static XmlRequirements aggregateReports(Path pBasePath) throws ReportException {

        // Scan all file tree from base directory
        try (Stream<Path> paths = Files.walk(pBasePath)) {
            List<Path> targetPaths = paths
                    .filter(path -> path.toFile().getName().startsWith(RequirementMatrixReportListener.REPORT_PREFIX))
                    .collect(Collectors.toList());
            for (Path p : targetPaths) {
                LOG.info(p.toString());
            }

            return aggregateReports(targetPaths);
        } catch (IOException e) {
            final String message = "Error scanning file tree";
            LOG.error(message, e);
            throw new ReportException(message);
        }
    }

    /**
     * Aggregate all reports in a single one
     * @param pReports list of reports
     * @return aggregated report
     * @throws ReportException if method cannot aggregate reports
     */
    public static XmlRequirements aggregateReports(List<Path> pReports)
            throws ReportException, UnsupportedEncodingException {
        // Init aggregation map
        final Map<String, XmlRequirement> rqmtMap = new HashMap<>();

        if (pReports != null) {
            for (Path reportPath : pReports) {
                final XmlRequirements tmp = XmlHelper.read(reportPath, XmlRequirements.class);
                if ((tmp != null) && (tmp.getRequirements() != null)) {
                    for (XmlRequirement rqmt : tmp.getRequirements()) {
                        aggregateTests(rqmtMap, rqmt);
                    }
                }
            }
        }

        // Compute result
        final XmlRequirements rqmts = new XmlRequirements();
        for (XmlRequirement rqmt : rqmtMap.values()) {
            rqmts.addRequirement(rqmt);
        }
        return rqmts;
    }

    /**
     * Aggregate identical requirement tests in a single wrapper
     * @param pRqmtMap working map
     * @param pXmlRequirement requirement to aggregate
     */
    private static void aggregateTests(Map<String, XmlRequirement> pRqmtMap, XmlRequirement pXmlRequirement) {
        final XmlRequirement rqmt = pRqmtMap.get(pXmlRequirement.getRequirement());
        if (rqmt == null) {
            pRqmtMap.put(pXmlRequirement.getRequirement(), pXmlRequirement);
        } else {
            rqmt.addAllTests(pXmlRequirement.getTests());
        }
    }

    /**
     * Check if object is not null
     * @param pObject objet to check
     * @param pMessage error message
     * @throws ReportException if a report parameter is null
     */
    private static void assertNotNull(Object pObject, String pMessage) throws ReportException {
        if (pObject == null) {
            LOG.error(pMessage);
            throw new ReportException(pMessage);
        }
    }
}
