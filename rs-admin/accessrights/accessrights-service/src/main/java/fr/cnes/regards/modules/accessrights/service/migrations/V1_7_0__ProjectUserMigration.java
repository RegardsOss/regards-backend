package fr.cnes.regards.modules.accessrights.service.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
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

@Component
@Profile("!test")
public class V1_7_0__ProjectUserMigration extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1_7_0__ProjectUserMigration.class);

    private static final int RETRY_DELAY = 30;
    private static final String SELECT_USERS = "SELECT email FROM t_project_user";
    private static final String EMAIL_COLUMN = "email";

    private final IAccountsClient accountsClient;
    private final IExternalAuthenticationClient externalAuthenticationClient;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private String uniqueServiceProvider;

    public V1_7_0__ProjectUserMigration(IAccountsClient accountsClient, IExternalAuthenticationClient externalAuthenticationClient, IRuntimeTenantResolver runtimeTenantResolver) {
        this.accountsClient = accountsClient;
        this.externalAuthenticationClient = externalAuthenticationClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }


    @Override
    public void migrate(Context context) throws InterruptedException {

        LOGGER.info("Starting Java migration {}", this.getClass().getSimpleName());

        Connection connection = context.getConnection();

        init();

        Set<String> users = getUsers(connection);

        LOGGER.info("Found {} users to update", users.size());

        users.forEach(this::updateUser);

        LOGGER.info("Completed Java migration {}", this.getClass().getSimpleName());
    }

    private void init() throws InterruptedException {

        int maxAttempts = 3;
        int attempt = 0;

        while (attempt++ < maxAttempts) {

            try {

                FeignSecurityManager.asSystem();

                // Retrieve service providers and set uniqueServiceProvider only if there's only one
                ResponseEntity<PagedModel<EntityModel<ServiceProviderDto>>> response = externalAuthenticationClient.getServiceProviders();
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    PagedModel<EntityModel<ServiceProviderDto>> body = response.getBody();
                    if (body != null) {
                        List<ServiceProviderDto> serviceProviders = HateoasUtils.unwrapCollection(body.getContent());
                        if (serviceProviders.size() == 1) {
                            uniqueServiceProvider = serviceProviders.get(0).getName();
                        }
                        LOGGER.info("Successfully contacted rs-admin-instance");
                    }
                    break;
                }
            } catch (Exception e) {
                String error = "Unable to contact rs-admin-instance";
                LOGGER.error(error, e);
                throw new FlywayException(error);
            } finally {
                FeignSecurityManager.reset();
            }

            if (attempt < maxAttempts) {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            }
        }
    }

    private Set<String> getUsers(Connection connection) {

        Set<String> users = new HashSet<>();

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SELECT_USERS)) {
                while (resultSet.next()) {
                    users.add(resultSet.getString(EMAIL_COLUMN));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }

        return users;
    }

    private void updateUser(String email) {

        LOGGER.info("Updating groups for user {}", email);

        String error = "Unable to update user " + email;

        try {

            FeignSecurityManager.asSystem();

            // Link Project to Account
            ResponseEntity<Void> linkResponse = accountsClient.link(email, runtimeTenantResolver.getTenant());
            if (linkResponse == null || !linkResponse.getStatusCode().is2xxSuccessful()) {
                throw new FlywayException(error);
            }
            // Fetch Account and update origin IF account is external and there's a unique service provider for project
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient.retrieveAccounByEmail(email);
            if (accountResponse == null || !accountResponse.getStatusCode().is2xxSuccessful()) {
                throw new FlywayException(error);
            }
            Account account = HateoasUtils.unwrap(accountResponse.getBody());
            if (account == null) {
                throw new FlywayException(error);
            }
            if (account.isExternal() && uniqueServiceProvider != null) {
                account.setOrigin(uniqueServiceProvider);
                ResponseEntity<EntityModel<Account>> updateResponse = accountsClient.updateAccount(account.getId(), account);
                if (updateResponse == null || !updateResponse.getStatusCode().is2xxSuccessful()) {
                    throw new FlywayException(error);
                }
            }

        } catch (Exception e) {
            LOGGER.error(error, e);
            throw new FlywayException(error);
        } finally {
            FeignSecurityManager.reset();
        }

    }

}
