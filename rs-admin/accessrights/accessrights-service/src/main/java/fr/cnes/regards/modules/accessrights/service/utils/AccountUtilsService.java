package fr.cnes.regards.modules.accessrights.service.utils;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
@RegardsTransactional
public class AccountUtilsService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountUtilsService.class);

    private final IAccountsClient accountsClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final Gson gson;

    public AccountUtilsService(IAccountsClient accountsClient,
                               IRuntimeTenantResolver runtimeTenantResolver,
                               Gson gson) {
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.gson = gson;
    }

    public Account retrieveAccount(String email) throws EntityInvalidException {
        Account account = null;
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient.retrieveAccountByEmail(email);
            if (accountResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
                EntityModel<Account> body = accountResponse.getBody();
                if (body != null) {
                    account = body.getContent();
                }
            }
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        } finally {
            FeignSecurityManager.reset();
        }
        return account;
    }

    public Account createAccount(AccessRequestDto accessRequestDto, boolean isExternalAccess, AccountStatus status)
        throws EntityException {

        String email = accessRequestDto.getEmail();
        Account createdAccount = null;

        try {
            FeignSecurityManager.asSystem();

            String firstName = accessRequestDto.getFirstName();
            String lastName = accessRequestDto.getLastName();
            String origin = accessRequestDto.getOrigin();
            String password = isExternalAccess ? null : accessRequestDto.getPassword();

            // Check input values
            boolean isValid = StringUtils.isNoneBlank(email, firstName, lastName);
            if (isExternalAccess) {
                // External accounts should have origin properly set
                isValid &= StringUtils.isNotBlank(origin) && !Account.REGARDS_ORIGIN.equals(origin);
            } else {
                // Regards' accounts need a non-blank password
                isValid &= StringUtils.isNotBlank(password);
            }
            if (!isValid) {
                LOG.error("Account does not exist for user {} and there is not enough information to create a new one.",
                          email);
                throw new EntityNotFoundException(email, Account.class);
            }

            Account account = new Account(email, firstName, lastName, null);

            // Set status - for admin use only, otherwise default is used
            if (status != null) {
                account.setStatus(status);
            }
            // Set origin - for external accounts only
            if (isExternalAccess) {
                account.setOrigin(accessRequestDto.getOrigin());
            }

            // Add password to account creation payload
            AccountNPassword accountNPassword = new AccountNPassword(account, password);

            EntityModel<Account> body = accountsClient.createAccount(accountNPassword).getBody();
            if (body != null) {
                createdAccount = body.getContent();
            }

        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        } finally {
            FeignSecurityManager.reset();
        }

        return createdAccount;
    }

    public void addProject(String email) throws EntityInvalidException {
        try {
            FeignSecurityManager.asSystem();
            accountsClient.link(email, runtimeTenantResolver.getTenant());
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOG.error(e.getMessage(), e);
            ServerErrorResponse errorResponse = gson.fromJson(e.getResponseBodyAsString(), ServerErrorResponse.class);
            throw new EntityInvalidException(errorResponse.getMessages());
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
