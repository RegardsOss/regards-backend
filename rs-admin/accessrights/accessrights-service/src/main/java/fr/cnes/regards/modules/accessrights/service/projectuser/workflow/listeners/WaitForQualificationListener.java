/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountAcceptedEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;

/**
 * Listen to {@link AccountAcceptedEvent} in order to pass {@link ProjectUser}s from WAITING_ACCOUNT_ACTIVE to WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class WaitForQualificationListener
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<AccountAcceptedEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(WaitForQualificationListener.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IInstanceSubscriber instanceSubscriber;

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Account workflow manager
     */
    private final ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * {@link IAccessSettingsService} instance
     */
    private final IAccessSettingsService accessSettingsService;

    /**
     * @param pProjectUserRepository
     * @param pProjectUserWorkflowManager
     * @param pAccessSettingsService
     */
    public WaitForQualificationListener(IProjectUserRepository pProjectUserRepository,
            ProjectUserWorkflowManager pProjectUserWorkflowManager, IAccessSettingsService pAccessSettingsService) {
        super();
        projectUserRepository = pProjectUserRepository;
        projectUserWorkflowManager = pProjectUserWorkflowManager;
        accessSettingsService = pAccessSettingsService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        instanceSubscriber.subscribeTo(AccountAcceptedEvent.class, this);
    }

    /**
     * Pass a {@link ProjectUser} from WAITING_ACCOUNT_ACTIVATION to WAITING_ACCESS
     * @param wrapper the event
     */
    @Override
    public void handle(TenantWrapper<AccountAcceptedEvent> wrapper) {
        // Retrieve the account/project user email
        String email = wrapper.getContent().getAccountEmail();
        LOG.info("Account accepted event received for user {}.", email);
        // Now for each tenant, lets handle this account activation
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            onAccountActivation(email);
            runtimeTenantResolver.clearTenant();
        }
    }

    public void onAccountActivation(String email) {
        // Retrieve the project user
        Optional<ProjectUser> optional = projectUserRepository.findOneByEmail(email);
        ProjectUser projectUser = optional.orElse(null);
        if (projectUser != null) {
            // Change state
            try {
                projectUserWorkflowManager.makeWaitForQualification(projectUser);

                // Auto-accept if configured so
                final AccessSettings settings = accessSettingsService.retrieve();
                if (AccessSettings.AUTO_ACCEPT_MODE.equals(settings.getMode())) {
                    projectUserWorkflowManager.grantAccess(projectUser);
                }

                // Save
                projectUserRepository.save(projectUser);
            } catch (EntityException e) {
                LOG.warn(String.format("The system tried to set the project user %s state to %s from %s but failed",
                                       email, UserStatus.WAITING_ACCESS, UserStatus.WAITING_ACCOUNT_ACTIVE),
                         e);
            }
        }
    }

}