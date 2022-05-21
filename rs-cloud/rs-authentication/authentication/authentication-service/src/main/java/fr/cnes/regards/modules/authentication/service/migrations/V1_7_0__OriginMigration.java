package fr.cnes.regards.modules.authentication.service.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Profile("!test")
public class V1_7_0__OriginMigration extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1_7_0__OriginMigration.class);

    private static final String UNKNOWN_PROVIDER = "Unknown Service Provider";

    private static final int RETRY_DELAY = 30;

    private static final String SELECT_SERVICE_PROVIDERS = "SELECT name FROM t_service_provider";

    private static final String NAME_COLUMN = "name";

    private final IAccountsClient accountsClient;

    private final IProjectUsersClient projectUsersClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public V1_7_0__OriginMigration(IAccountsClient accountsClient,
                                   IProjectUsersClient projectUsersClient,
                                   IRuntimeTenantResolver runtimeTenantResolver) {
        this.accountsClient = accountsClient;
        this.projectUsersClient = projectUsersClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void migrate(Context context) throws InterruptedException {

        LOGGER.info("Starting Java migration {}", this.getClass().getSimpleName());

        Connection connection = context.getConnection();

        checkAdminInstanceServiceCanBeAccessed();
        checkAdminServiceCanBeAccessed();

        Set<String> serviceProviders = getServiceProviders(connection);

        switch (serviceProviders.size()) {
            case 0:
                LOGGER.info("No Service Provider configured : nothing to do here");
                break;
            case 1:
                String uniqueServiceProvider = serviceProviders.iterator().next();
                LOGGER.info("Found exactly 1 Service Provider : updating external users with \"{}\"",
                            uniqueServiceProvider);
                updateExternalUsers(uniqueServiceProvider);
                break;
            default:
                LOGGER.info("Found more than 1 Service Provider : updating external users with generic origin");
                updateExternalUsers(UNKNOWN_PROVIDER);

        }

        LOGGER.info("Completed Java migration {}", this.getClass().getSimpleName());
    }

    private void checkAdminServiceCanBeAccessed() throws InterruptedException {

        int maxAttempts = 3;
        int attempt = 0;

        while (attempt++ < maxAttempts) {

            try {
                FeignSecurityManager.asSystem();
                ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response = projectUsersClient.retrieveProjectUserList(
                    new ProjectUserSearchParameters(),
                    PageRequest.of(0, 1));
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    PagedModel<EntityModel<ProjectUser>> body = response.getBody();
                    if (body != null) {
                        LOGGER.info("Successfully contacted rs-admin");
                    }
                    break;
                }
            } catch (Exception e) {
                String error = "Unable to contact rs-admin";
                LOGGER.error(error, e);
                if (attempt >= maxAttempts) {
                    throw new FlywayException(error);
                }
            } finally {
                FeignSecurityManager.reset();
            }

            if (attempt < maxAttempts) {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            }
        }
    }

    private void checkAdminInstanceServiceCanBeAccessed() throws InterruptedException {

        int maxAttempts = 3;
        int attempt = 0;

        while (attempt++ < maxAttempts) {

            try {
                FeignSecurityManager.asSystem();
                ResponseEntity<PagedModel<EntityModel<Account>>> response = accountsClient.retrieveAccountList(new AccountSearchParameters(),
                                                                                                               0,
                                                                                                               1);
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    PagedModel<EntityModel<Account>> body = response.getBody();
                    if (body != null) {
                        LOGGER.info("Successfully contacted rs-admin-instance");
                    }
                    break;
                }
            } catch (Exception e) {
                String error = "Unable to contact rs-admin-instance";
                LOGGER.error(error, e);
                if (attempt >= maxAttempts) {
                    throw new FlywayException(error);
                }
            } finally {
                FeignSecurityManager.reset();
            }

            if (attempt < maxAttempts) {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            }
        }
    }

    private Set<String> getServiceProviders(Connection connection) {

        Set<String> serviceProviders = new HashSet<>();

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SELECT_SERVICE_PROVIDERS)) {
                while (resultSet.next()) {
                    serviceProviders.add(resultSet.getString(NAME_COLUMN));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }

        return serviceProviders;
    }

    private void updateExternalUsers(String origin) {

        try {

            FeignSecurityManager.asSystem();

            AccountSearchParameters accountSearchParameters = new AccountSearchParameters().setProject(
                runtimeTenantResolver.getTenant());
            List<Account> projectAccounts = HateoasUtils.retrieveAllPages(100,
                                                                          pageable -> accountsClient.retrieveAccountList(
                                                                              accountSearchParameters,
                                                                              pageable.getPageNumber(),
                                                                              pageable.getPageSize()));
            List<String> externalAccounts = projectAccounts.stream()
                                                           .filter(Account::isExternal)
                                                           .map(Account::getEmail)
                                                           .collect(Collectors.toList());

            LOGGER.info("Found {} users to update", externalAccounts.size());

            externalAccounts.forEach(email -> {

                LOGGER.info("Updating origin for user {}", email);
                String error = "Unable to update user " + email;

                ResponseEntity<Void> updateResponse = accountsClient.updateOrigin(email, origin);
                if (updateResponse == null || !updateResponse.getStatusCode().is2xxSuccessful()) {
                    throw new FlywayException(error);
                }

                updateResponse = projectUsersClient.updateOrigin(email, origin);
                if (updateResponse == null || !updateResponse.getStatusCode().is2xxSuccessful()) {
                    throw new FlywayException(error);
                }

            });

        } catch (Exception e) {
            LOGGER.error("Unable to update users", e);
            throw new FlywayException("Unable to update users");
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
