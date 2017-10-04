/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.dao.IStorageParameterRepository;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 *
 * Scheduler to calculate update rate of stored metadata.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@EnableScheduling
@Order
public class UpdateMetadataScheduler implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateMetadataScheduler.class);

    /**
     * Resolver to know all existing tenants of the current REGARDS instance.
     */
    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IStorageParameterRepository storageParameterRepo;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Bean(destroyMethod = "shutdown")
    public Executor updateMetadataTaskExecutor() {
        return Executors.newScheduledThreadPool(1);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(updateMetadataTaskExecutor());
        // TODO : Handle new tenants.
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            LOG.debug(" ----------------------------------------> Initialize new UpdateMetadata scheduled Tasks for tenant {}.",
                      tenant);
            taskRegistrar
                    .addTriggerTask(new UpdateMetadataScheduledTask(tenant, aipService, runtimeTenantResolver),
                                    new UpdateMetadataTrigger(tenant, runtimeTenantResolver, storageParameterRepo));
            LOG.debug("Initializion for tenant {} done.", tenant);
        }
    }

}
