/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PasswordResetToken;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.project.service.ITenantService;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status LOCKED.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
@InstanceTransactional
public class LockedState extends AbstractDeletableState {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LockedState.class);

    /**
     * The account unlock email template code
     */
    private static final String ACCOUNT_UNLOCK_RESET_TEMPLATE = "accountUnlockTemplate";

    /**
     * CRUD service handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountService accountService;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param projectUsersClient
     * @param accountRepository
     * @param tenantService
     * @param runtimeTenantResolver
     * @param passwordResetService
     * @param accountUnlockTokenService
     * @param accountService
     * @param templateService
     * @param emailClient
     */
    public LockedState(IProjectUsersClient projectUsersClient, IAccountRepository accountRepository,
            ITenantService tenantService, IRuntimeTenantResolver runtimeTenantResolver,
            IPasswordResetService passwordResetService,
            IAccountUnlockTokenService accountUnlockTokenService, IAccountService accountService,
            ITemplateService templateService, IEmailClient emailClient) {
        super(projectUsersClient, accountRepository, tenantService, runtimeTenantResolver,
              passwordResetService, accountUnlockTokenService);
        this.accountService = accountService;
        this.templateService = templateService;
        this.emailClient = emailClient;
    }

    @Override
    public void requestUnlockAccount(final Account pAccount, final String pOriginUrl, final String pRequestLink) {
        // Create the token
        final String token = accountUnlockTokenService.create(pAccount);

        // Build the list of recipients
        final String[] recipients = { pAccount.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", pAccount.getFirstName());
        data.put("requestLink", pRequestLink);
        data.put("originUrl", pOriginUrl);
        data.put("token", token);
        data.put("accountEmail", pAccount.getEmail());

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TemplateServiceConfiguration.ACCOUNT_UNLOCK_TEMPLATE_CODE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template to generate a unlock account email. Falling back to default.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Unlock account");

            String linkUrlTemplate;
            if (pRequestLink.contains("?")) {
                linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
            } else {
                linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
            }
            final String linkUrl = String.format(linkUrlTemplate, pRequestLink, pOriginUrl, token, pAccount.getEmail());
            email.setText("Please click on the following link unlock your account: " + linkUrl);
        }

        // Send it
        FeignSecurityManager.asSystem();
        emailClient.sendEmail(email);
        FeignSecurityManager.reset();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#performUnlockAccount(fr.cnes.regards.
     * modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void performUnlockAccount(final Account account, final String pToken) throws EntityException {
        validateToken(account.getEmail(), pToken);
        account.setStatus(AccountStatus.ACTIVE);
        accountService.updateAccount(account.getId(), account);
        accountService.resetAuthenticationFailedCounter(account.getId());
        accountUnlockTokenService.deleteAllByAccount(account);
    }

    /**
     * Validate the token
     *
     * @param pAccountEmail
     *            the account email
     * @param pToken
     *            the token to validate
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} when the token is not linked to the passed account or is
     *             expired<br>
     *             {@link EntityNotFoundException} when the token dos not exist
     */
    private void validateToken(final String pAccountEmail, final String pToken) throws EntityException {
        // Retrieve the token object
        final AccountUnlockToken token = accountUnlockTokenService.findByToken(pToken);

        // Check same account
        if (!token.getAccount().getEmail().equals(pAccountEmail)) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Invalid token");
        }

        // Check token expiry
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Expired token");
        }
    }

}
