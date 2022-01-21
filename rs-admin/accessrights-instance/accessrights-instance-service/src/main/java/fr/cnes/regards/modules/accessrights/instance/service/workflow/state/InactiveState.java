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

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.project.service.ITenantService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status INACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
@InstanceTransactional
public class InactiveState extends AbstractDeletableState {

    /**
     * In days. Provided by property file.
     */
    private final Long accountValidityDuration;

    /**
     * @param projectUsersClient
     * @param accountRepository
     * @param tenantService
     * @param runtimeTenantResolver
     * @param passwordResetService
     * @param accountUnlockTokenService
     */
    public InactiveState(IProjectUsersClient projectUsersClient, IAccountRepository accountRepository,
            ITenantService tenantService, IRuntimeTenantResolver runtimeTenantResolver,
            IPasswordResetService passwordResetService, IAccountUnlockTokenService accountUnlockTokenService,
            @Value("${regards.accounts.validity.duration}") Long accountValidityDuration) {
        super(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver, passwordResetService,
              accountUnlockTokenService);
        this.accountValidityDuration = accountValidityDuration;
    }

    @Override
    public void activeAccount(final Account pAccount) {
        pAccount.setStatus(AccountStatus.ACTIVE);
        pAccount.setInvalidityDate(LocalDateTime.now().plusDays(accountValidityDuration));
        accountRepository.save(pAccount);
    }

}
