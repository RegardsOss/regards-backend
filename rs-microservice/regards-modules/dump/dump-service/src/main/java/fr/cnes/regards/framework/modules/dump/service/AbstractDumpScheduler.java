package fr.cnes.regards.framework.modules.dump.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.IDumpSettingsService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public abstract class AbstractDumpScheduler extends AbstractTaskScheduler {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    private Map<String, ScheduledFuture> schedulersByTenant = Maps.newHashMap();

    /**
     * Create schedulers when the application context has been refreshed see {@link ApplicationStartedEvent}
     */
    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        initSchedulers();
    }

    /**
     * Initialize scheduled {@link FeatureSaveMetadataJobTask}s for all tenants to save feature metadata
     */
    public void initSchedulers() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            updateScheduler(tenant);
        }
    }

    /**
     * Create a new scheduled {@link FeatureSaveMetadataJobTask} for the tenant created
     * @param event to inform of a new connection see {@link TenantConnectionReady}
     */
    @EventListener
    public void onTenantConnectionReady(TenantConnectionReady event) {
        updateScheduler(event.getTenant());
    }

    /**
     * Update the scheduler configured for the tenant
     * Cancel the previous {@link FeatureSaveMetadataJobTask} (if existing) and put a new task
     * @param tenant tenant to be updated
     */
    public void updateScheduler(String tenant) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            DumpSettings newDumpSettings = dumpSettingsService.retrieve();
            //FIXME dire que l'on update le scheduler en info (utiliser getLogger)
            traceScheduling(tenant, getType());
            // cancel the existing scheduler, wait until the end to cancel it
            ScheduledFuture schedulerToRestart = schedulersByTenant.get(tenant);
            if (schedulerToRestart != null) {
                schedulerToRestart.cancel(false);
            }
            // update scheduler only if the module is active
            if (newDumpSettings.isActiveModule()) {
                schedulersByTenant.put(tenant, taskScheduler.schedule(() -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    traceScheduling(tenant, getType());
                    try {
                        lockingTaskExecutors.executeWithLock(getDumpTask(),
                                                             new LockConfiguration(getLockName(),
                                                                                   Instant.now().plusSeconds(60L)));
                    } catch (Throwable e) {
                        handleSchedulingError(getType(), getNotificationTitle(), e);
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                }, new CronTrigger(newDumpSettings.getCronTrigger())));
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    public Map<String, ScheduledFuture> getSchedulersByTenant() {
        return schedulersByTenant;
    }

    protected abstract String getLockName();

    protected abstract LockingTaskExecutor.Task getDumpTask();

    protected abstract String getNotificationTitle();

    protected abstract String getType();


    /**
     * Update the {@link DumpSettings} and the {@link AbstractDumpScheduler} with the new configuration
     * @param newDumpSettings the new dump configuration
     */
    @RegardsTransactional
    public void updateDumpAndScheduler(DumpSettings newDumpSettings) throws ModuleException {
        // PARAMETER CHECK
        // check cron expression
        String cronTrigger = newDumpSettings.getCronTrigger();
        if (!CronSequenceGenerator.isValidExpression(cronTrigger)) {
            throw new EntityInvalidException(String.format("Cron Expression %s is not valid.", cronTrigger));
        }

        // UPDATE DUMP SETTINGS if they already exist
        dumpSettingsService.update(newDumpSettings);

        // UPDATE SCHEDULER
        updateScheduler(runtimeTenantResolver.getTenant());
    }
}
