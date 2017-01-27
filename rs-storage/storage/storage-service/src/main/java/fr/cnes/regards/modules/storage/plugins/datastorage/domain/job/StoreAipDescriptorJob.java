/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.domain.job;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameters;
import fr.cnes.regards.framework.modules.jobs.domain.Output;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class StoreAipDescriptorJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(StoreAipDescriptorJob.class);

    private static final String PARAMETER_MISSING = "%s requires a %s as \"%s\" parameter";

    private static final String PARAMETER_INVALID = "%s requires a valid %s";

    @Override
    public int getPriority() {
        // FIXME: it is almost always a synchronous call so priority should be higher?
        return 0;
    }

    @Override
    public List<Output> getResults() {
        // TODO Auto-generated method stub
        // outputs are checksums? and new URI?
        return null;
    }

    @Override
    public StatusInfo getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasResult() {
        // TODO Auto-generated method stub
        // only if results are what is specified in #getResults
        return false;
    }

    @Override
    public boolean needWorkspace() {
        // TODO Auto-generated method stub
        // depends on the storage used
        return false;
    }

    @Override
    public void run() {

    }

    /**
     * @throws JobParameterMissingException
     * @throws JobParameterInvalidException
     *
     */
    private void areAllParametersNeededSpecified() throws JobParameterMissingException, JobParameterInvalidException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Map<String, Object> specifiedParameters = parameters.getParameters();
        Object possibleDest = specifiedParameters.get("destination");
        if (!(possibleDest instanceof URL)) {
            throw new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), URL.class.getName(), "destination"));
        }
        Object possibleAip = specifiedParameters.get("AIP");
        if (!(possibleAip instanceof AIP)) {
            throw new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), AIP.class.getName(), "AIP"));
        }
        AIP aip = (AIP) possibleAip;
        if (validator.validate(aip).size() != 0) {
            throw new JobParameterInvalidException(
                    String.format(PARAMETER_INVALID, this.getClass().getName(), AIP.class.getName()));
        }
    }

    @Override
    public void setParameters(JobParameters pParameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        parameters = pParameters;
        areAllParametersNeededSpecified();
    }

}
