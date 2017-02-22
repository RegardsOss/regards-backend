/*
 * LICENSE_PLACEHOLDER
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
    public static void main(String[] args) {

        LOGGER.info("Starting report aggregation");

        // Check argument length
        if ((args == null) || (args.length != 1)) {
            usage();
        }

        // Check basedir exists
        Path basePath = Paths.get(args[0]);
        if (!Files.isDirectory(basePath)) {
            LOGGER.error("Basedir {} does not exists or is not a directory", basePath);
            usage();
        }

        Aggregator a = new Aggregator(basePath);
        try {
            a.aggregate();
        } catch (AggregationException | ReportException e) {
            LOGGER.info("Aborting report aggregation");
            System.exit(-1);
        }

        LOGGER.info("Report aggregation finished successfully");
    }

    public static void usage() {
        LOGGER.info("Usage : java -jar {} <basedir>");
        System.exit(-1);
    }

}
