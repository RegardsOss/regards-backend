/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.jobs.domain;

/**
 *
 * Utility class to manage a {@link JobParameters}.
 *
 * @author Christophe Mertz
 */
public class JobParametersFactory {

    /**
     * 
     */
    private JobParameters parameters;

    /**
     * Constructor
     *
     */
    public JobParametersFactory() {
        parameters = new JobParameters();
    }

    /**
     *
     * Build a new class
     *
     * @return a factory
     */
    public static JobParametersFactory build() {
        return new JobParametersFactory();
    }

    /**
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * 
     * @param pParameterValue
     *            the value parameter
     * 
     * @return the factory
     */
    public JobParametersFactory addParameter(String pParameterName, Object pParameterValue) {
        parameters.add(pParameterName, pParameterValue);
        return this;
    }

    public JobParameters getParameters() {
        return parameters;
    }
}
