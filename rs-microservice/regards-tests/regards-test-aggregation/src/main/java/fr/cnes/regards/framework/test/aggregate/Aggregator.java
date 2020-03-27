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
package fr.cnes.regards.framework.test.aggregate;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.exception.ReportException;
import fr.cnes.regards.framework.test.report.xls.XlsxHelper;
import fr.cnes.regards.framework.test.report.xml.XmlHelper;
import fr.cnes.regards.framework.test.report.xml.XmlRequirements;

/**
 * This class aggregates Junit reports per microservices. Each microservice reports has to be stored in its own
 * directory.<br/>
 * Aggregate reports are written in an XLSX document with each sheet representing a microservice.
 * @author Marc Sordi
 */
public class Aggregator {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregate.class);

    private final Path basePath;

    public Aggregator(Path pBasePath) {
        this.basePath = pBasePath;
    }

    public void aggregate(Path destinationDir) throws AggregationException, ReportException {

        // From base directory, list all contained directories
        List<Path> jobPaths = getJobPaths();

        // Initialize result file
        String radical = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        Path result = destinationDir.resolve(radical + ".xlsx");

        LOGGER.info("Aggregating reports in file {}", result);

        for (Path jobPath : jobPaths) {
            XmlRequirements rqmts = XmlHelper.aggregateReports(jobPath);
            String sheetName = jobPath.getFileName().toString();
            XlsxHelper.write(result, rqmts, sheetName);
        }
    }

    private List<Path> getJobPaths() throws AggregationException {
        List<Path> p = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(basePath)) {
            for (Path path : directoryStream) {
                LOGGER.info("Directory detected : {}", path);
                p.add(path);
            }
        } catch (IOException e) {
            String m = String.format("Error listing base directory %s", basePath);
            LOGGER.error(m, e);
            throw new AggregationException(m, e);
        }
        return p;
    }
}
