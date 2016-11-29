/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.TenantUtils;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * Abstract state implementation to implement the delete action on an account.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
abstract class AbstractDeletableState implements IAccountTransitions {

    /**
     * Service managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserService projectUserService;

    /**
     * Repository
     */
    private final IAccountRepository accountRepository;

    /**
     * JWT Service. Autowired by Spring.
     */
    private final JWTService jwtService;

    /**
     * Tenant resolver
     */
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
    public void delete(final Account pAccount) throws ModuleException {
        switch (pAccount.getStatus()) {
            case ACTIVE:
            case LOCKED:
            case INACTIVE:
                doDelete(pAccount);
                break;
            default:
                throw new EntityTransitionForbiddenException(pAccount.getId().toString(), ProjectUser.class,
                        pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * Delete an accont
     *
     * @param pAccount
     *            the account
     * @throws EntityOperationForbiddenException
     *             when the account is linked to at least a project user
     */
    private void doDelete(final Account pAccount) throws EntityOperationForbiddenException {
        // Get all tenants
        final Set<String> tenants = tenantResolver.getAllTenants();

        // Is there a project user associated to the account on the current tenant?
        final BooleanSupplier hasProjectUser = () -> projectUserService.existUser(pAccount.getEmail());

        // Is there a project user associated to the account on the passed tenant?
        final Predicate<String> hasProjectUserOnTenant = TenantUtils.asSafeCallableOnTenant(hasProjectUser);

        try (Stream<String> stream = tenants.stream()) {
            if (stream.anyMatch(hasProjectUserOnTenant)) {
                throw new EntityOperationForbiddenException(pAccount.getId().toString(), Account.class,
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
