package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * A class that wait
 * @author oroussel
 */
public class WaiterJob extends AbstractJob<Void> {

    public static final String WAIT_PERIOD_COUNT = "waitPeriodCount";

    public static final String WAIT_PERIOD = "waitPeriod";

    private Integer waitPeriodCount;

    private Long waitPeriod;

    public WaiterJob() {
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        for (JobParameter param : parameters.values()) {
            switch (param.getName()) {
                case WAIT_PERIOD:
                    try {
                        this.waitPeriod = param.getValue();
                    } catch (NumberFormatException nfe) {
                        throw new JobParameterInvalidException(WAIT_PERIOD, nfe);
                    }
                    break;
                case WAIT_PERIOD_COUNT:
                    try {
                        this.waitPeriodCount = param.getValue();
                    } catch (NumberFormatException nfe) {
                        throw new JobParameterInvalidException(WAIT_PERIOD_COUNT, nfe);
                    }
                    break;
            }
        }
        if (this.waitPeriod == null) {
            throw new JobParameterMissingException(WAIT_PERIOD);
        } else if (this.waitPeriodCount == null) {
            throw new JobParameterMissingException(WAIT_PERIOD_COUNT);
        }
    }

    @Override
    public int getCompletionCount() {
        return waitPeriod.intValue();
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < waitPeriodCount; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("INTERRUPTED in loop");
                    break;
                }
                System.out.println("START WAITING...");
                Thread.sleep(waitPeriod);
                advanceCompletion();
                System.out.println("... END WAITING");
            }
        } catch (InterruptedException e) {
            System.out.println("INTERRUPTED in sleep");
        }
    }
}
