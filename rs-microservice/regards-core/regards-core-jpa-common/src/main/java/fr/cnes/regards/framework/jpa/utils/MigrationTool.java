/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

/**
 * Available migration tools<br/>
 * {@link MigrationTool#HBM2DDL} is intended to be used for test purpose.<br/>
 * {@link MigrationTool#FLYWAYDB} is intended to be used in production.
 * @author Marc Sordi
 *
 */
public enum MigrationTool {
    HBM2DDL,
    FLYWAYDB;
}
