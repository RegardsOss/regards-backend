/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 * and {@link StepPropertyUpdateRequest }
 *
 * @author Iliana Ghazali
 */
@ConditionalOnProperty(value = "regards.microservices.agent.snapshot.enabled", matchIfMissing = true)
@EnableScheduling
public class AgentCleanSessionStepScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSessionStepScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AgentCleanSessionStepJobService agentCleanSessionStepJobService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Value("${spring.application.name}")
    private String microserviceName;

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String DEFAULT_INITIAL_DELAY = "10000";

    public static final String DEFAULT_SCHEDULING_DELAY = "86400000"; // once a day

    public static final String CLEAN_SESSION_STEPS = "Clean session steps";

    public static final String CLEAN_SESSION_STEPS_TITLE = "Clean session steps scheduling";

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task cleanProcessTask = () -> {
        lockingTaskExecutors.assertLocked();
        agentCleanSessionStepJobService.scheduleJob();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = "${regards.session.agent.clean.session.step.scheduler.bulk.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.session.agent.clean.session.step.scheduler.bulk.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    protected void scheduleCleanSessionStep() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, CLEAN_SESSION_STEPS);
                
                lockingTaskExecutors.executeWithLock(cleanProcessTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           microserviceName + "_clean-session-steps",
                                                                           Duration.ofSeconds(MAX_TASK_DELAY),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(CLEAN_SESSION_STEPS, CLEAN_SESSION_STEPS_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
