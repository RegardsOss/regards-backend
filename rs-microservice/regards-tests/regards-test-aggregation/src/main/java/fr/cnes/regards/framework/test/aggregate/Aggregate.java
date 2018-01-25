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
package fr.cnes.regards.framework.test.aggregate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.exception.ReportException;

/**
 * Launch report aggregation
 *
 * @author Marc Sordi
 *
 */
public class Aggregate {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregate.class);

    /**
     * @param args
     */
    public static void main(String[] args) throws AggregationException, ReportException {

        LOGGER.info("Starting report aggregation");

        // Check argument length
        if ((args == null) || (args.length != 2)) {
            usage();
            return;
        }

        // Check basedir exists
        Path basePath = Paths.get(args[0]);
        if (!Files.isDirectory(basePath)) {
            LOGGER.error("Basedir {} does not exists or is not a directory", basePath);
            usage();
        }

        // Get destination directory and check if it exists
        Path destinationDir = Paths.get(args[1]);
        if (!Files.isDirectory(basePath)) {
            LOGGER.error("Destination directory {} does not exists or is not a directory", destinationDir);
            usage();
        }

        Aggregator a = new Aggregator(basePath);
        try {
            a.aggregate(destinationDir);
        } catch (AggregationException | ReportException e) {
            LOGGER.info("Aborting report aggregation");
            throw e;
        }

        LOGGER.info("Report aggregation finished successfully");
    }

    public static void usage() {
        LOGGER.info("Usage : java -jar {} <basedir> <destinationDir>");
    }

}
