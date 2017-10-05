/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.dao.IStorageParameterRepository;
import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;
import fr.cnes.regards.modules.storage.service.parameter.IStorageParameterService;

/**
 * {@link Trigger} to calculate next executation time of the {@link UpdateMetadataScheduledTask}.
 * @author Sébastien Binda
 */
public class UpdateMetadataTrigger implements Trigger {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateMetadataTrigger.class);

    /**
     * Tenant for the current scheduled task.
     */
    private final String tenant;

    private final IStorageParameterRepository storageParameterRepo;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public UpdateMetadataTrigger(String pTenant, IRuntimeTenantResolver pRuntimeTenantResolver,
            IStorageParameterRepository pStorageParameterRepo) {
        super();
        LOG.debug("New UpdateMetadataTrigger for tenant {}", pTenant);
        tenant = pTenant;
        runtimeTenantResolver = pRuntimeTenantResolver;
        storageParameterRepo = pStorageParameterRepo;
    }

    /**
     * Calculate next execution date for the updateAlreadyStoredMetadata execution.<br/>
     * The next execution date is calculated as :
     * <ul>
     * <li>At first run : Fixed date with {@link IStorageParameterService#DEFAULT_UPDATE_RATE}</li>
     * <li>Next runs will be fired every one minute {@link Calendar#MINUTE}</li>
     * </ul>
     *
     * @param pTriggerContext {@link TriggerContext}
     * @return {@link Date} next executation date.
     */
    @Override
    public Date nextExecutionTime(TriggerContext pTriggerContext) {
        LOG.debug("Caculate next update metadata task run date for tenant {}", tenant);
        runtimeTenantResolver.forceTenant(tenant);
        Calendar nextExecutionTime = new GregorianCalendar();
        Date lastActualExecutionTime = pTriggerContext.lastActualExecutionTime();
        nextExecutionTime.setTime(lastActualExecutionTime != null ? lastActualExecutionTime : new Date());
        // on first start of the microservice, the database might not have been initialiazed yet,
        // so let set the default value. Otherwise lets get it from the DB
        StorageParameter updateRate;
        if ((updateRate = storageParameterRepo.findOneByName(StorageParameter.UPDATE_RATE)) == null) {
            updateRate = new StorageParameter(StorageParameter.UPDATE_RATE,
                    IStorageParameterService.DEFAULT_UPDATE_RATE);
        }
        int updateRateFromDB = Integer.parseInt(updateRate.getValue());
        nextExecutionTime.add(Calendar.MINUTE, updateRateFromDB);
        runtimeTenantResolver.clearTenant();
        return nextExecutionTime.getTime();
    }

}
