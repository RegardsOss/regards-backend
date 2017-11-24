package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * {@link JobParameter} specific class to be used with  PostAcquisitionJob
 * 
 * @author Christophe Mertz
 */
public class SIPEventJobParameter extends JobParameter {

    public static final String NAME = "sip-event";

    public SIPEventJobParameter(SIPEvent value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getValue() {
        return super.getValue();
    }

//    public void setValue(String value) {
//        super.setValue(value);
//    }

    /**
     * Check if given {@link JobParameter} is compatible with SIPEventJobParameter ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
