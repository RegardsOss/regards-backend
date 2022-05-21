/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.events.OnRefuseAccountEvent;
import fr.cnes.regards.modules.project.service.ITenantService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status PENDING.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
@InstanceTransactional
public class PendingState extends AbstractDeletableState {

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    private final IAccountService accountService;

    /**
     * @param projectUsersClient
     * @param accountRepository
     * @param tenantService
     * @param runtimeTenantResolver
     * @param passwordResetService
     * @param accountUnlockTokenService
     * @param eventPublisher
     * @param accountService
     */
    public PendingState(IProjectUsersClient projectUsersClient,
                        IAccountRepository accountRepository,
                        ITenantService tenantService,
                        IRuntimeTenantResolver runtimeTenantResolver,
                        IPasswordResetService passwordResetService,
                        IAccountUnlockTokenService accountUnlockTokenService,
                        ApplicationEventPublisher eventPublisher,
                        IAccountService accountService) {
        super(projectUsersClient,
              accountRepository,
              tenantService,
              runtimeTenantResolver,
              passwordResetService,
              accountUnlockTokenService);
        this.eventPublisher = eventPublisher;
        this.accountService = accountService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#acceptAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void acceptAccount(final Account pAccount) {
        accountService.activate(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#acceptAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void refuseAccount(final Account pAccount) throws EntityException {
        deleteLinkedProjectUsers(pAccount);
        deleteAccount(pAccount);
        eventPublisher.publishEvent(new OnRefuseAccountEvent(pAccount));
    }

}
