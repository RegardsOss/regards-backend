/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;

import fr.cnes.regards.modules.jobs.domain.converters.JobParameterConverter;

/**
 * @author Léo Mieulet
 *
 */
@Convert(converter = JobParameterConverter.class)
public class JobParameters {

    /**
     * store jobInfo parameters
     */
    private final Map<String, Object> parameters;

    /**
     * Default constructor
     */
    public JobParameters() {
        super();
        parameters = new HashMap<>();
    }

    /**
     * Define a job parameter
     *
     * @param pParamName
     *            The attribute name
     * @param pValue
     *            The attribute value
     */
    public void add(final String pParamName, final Object pValue) {
        parameters.put(pParamName, pValue);
    }

    /**
     * @return job parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

}
