/**
 *
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;

import fr.cnes.regards.modules.jobs.domain.converters.JobParameterConverter;

/**
 * @author lmieulet
 *
 */
@Convert(converter = JobParameterConverter.class)
public class JobParameters {

    private final Map<String, Object> parameters;

    /**
     *
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
     * @param value
     *            The attribute value
     */
    public void add(String pParamName, Object value) {
        parameters.put(pParamName, value);
    }

    /**
     * @return
     */
    public Map<String, Object> getMap() {
        return parameters;
    }

}
