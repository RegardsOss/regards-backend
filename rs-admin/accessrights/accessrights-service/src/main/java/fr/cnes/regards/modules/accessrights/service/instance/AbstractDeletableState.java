/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.instance;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.IProjectUserService;

/**
 * Abstract state implementation to implement the delete action on an account.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
abstract class AbstractDeletableState implements IAccountState {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeletableState.class);

    /**
     * Service managing {@link ProjectUser}s. Autowired by Spring.
     */
    @Autowired
    private final IProjectUserService projectUserService;

    /**
     * Repository
     */
    @Autowired
    private final IAccountRepository accountRepository;

    /**
     * JWT Service. Autowired by Spring.
     */
    @Autowired
    private final JWTService jwtService;

    /**
     * Tenant resolver
     */
    @Autowired
    private final ITenantResolver tenantResolver;

    /**
     * Constructor
     *
     * @param pProjectUserService
     *            the project user service
     * @param pAccountRepository
     *            the account repository
     * @param pJwtService
     *            the jwt service
     * @param pTenantResolver
     *            the tenant resolver
     */
    public AbstractDeletableState(final IProjectUserService pProjectUserService,
            final IAccountRepository pAccountRepository, final JWTService pJwtService,
            final ITenantResolver pTenantResolver) {
        super();
        projectUserService = pProjectUserService;
        accountRepository = pAccountRepository;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
    }

    @Override
    public void delete(final Account pAccount) throws OperationForbiddenException {
        switch (pAccount.getStatus()) {
            case ACTIVE:
            case LOCKED:
            case INACTIVE:
                doDelete(pAccount);
                break;
            default:
                throw new IllegalActionForAccountStatusException(pAccount,
                        Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * Delete an accont
     *
     * @param pAccount
     *            the account
     * @throws OperationForbiddenException
     *             when the account is linked to at least a project user
     */
    private void doDelete(final Account pAccount) throws OperationForbiddenException {
        // Get all tenants
        final Set<String> tenants = tenantResolver.getAllTenants();

        // Define inject tenant consumer
        final Consumer<? super String> injectTenant = t -> {
            // TODO: User role system
            jwtService.changeTenant(t);
            LOG.info("Injected tenant " + t);
        };

        // Predicate: is there a project user associated to the account on this tenant?
        final Predicate<? super String> hasProjectUser = t -> {
            LOG.info("Evaluated predicate with tenant " + t);
            return projectUserService.existUser(pAccount.getEmail());
        };

        try (Stream<String> stream = tenants.stream()) {
            if (stream.peek(injectTenant).anyMatch(hasProjectUser)) {
                throw new OperationForbiddenException(pAccount.getId().toString(), Account.class,
                        "Cannot remove account because it is linked to at least on project user.");
            } else {
                accountRepository.delete(pAccount.getId());
            }
        }
    }

    /**
     * @return the projectUserService
     */
    protected IProjectUserService getProjectUserService() {
        return projectUserService;
    }

    /**
     * @return the accountRepository
     */
    protected IAccountRepository getAccountRepository() {
        return accountRepository;
    }

    /**
     * @return the jwtService
     */
    protected JWTService getJwtService() {
        return jwtService;
    }

    /**
     * @return the tenantResolver
     */
    protected ITenantResolver getTenantResolver() {
        return tenantResolver;
    };

}
