package fr.cnes.regards.modules.accessrights.service.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
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

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
public class V1_7_0__ProjectUserMigration extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1_7_0__ProjectUserMigration.class);

    private static final int RETRY_DELAY = 30;
    private static final Instant NOW = Instant.now();

    private static final String EMAIL_COLUMN = "email";
    private static final String FIRSTNAME_COLUMN = "firstname";
    private static final String NAME_COLUMN = "lastname";
    private static final String CREATION_DATE_COLUMN = "created";
    private static final String SELECT_USERS = "SELECT " + EMAIL_COLUMN + " FROM t_project_user";
    private static final String UPDATE_USERS =
            "UPDATE t_project_user SET " + FIRSTNAME_COLUMN + "=?, " + NAME_COLUMN + "=?, " + CREATION_DATE_COLUMN + "=? WHERE " + EMAIL_COLUMN + "=?";

    private final IAccountsClient accountsClient;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public V1_7_0__ProjectUserMigration(IAccountsClient accountsClient, IRuntimeTenantResolver runtimeTenantResolver) {
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }


    @Override
    public void migrate(Context context) throws InterruptedException {

        LOGGER.info("Starting Java migration {}", this.getClass().getSimpleName());

        Connection connection = context.getConnection();

        checkAdminInstanceServiceCanBeAccessed();

        Set<String> users = getUsers(connection);

        LOGGER.info("Found {} users to update", users.size());

        users.forEach(email -> updateUser(email, connection));

        LOGGER.info("Completed Java migration {}", this.getClass().getSimpleName());
    }

    private void checkAdminInstanceServiceCanBeAccessed() throws InterruptedException {

        int maxAttempts = 3;
        int attempt = 0;

        while (attempt++ < maxAttempts) {

            try {
                FeignSecurityManager.asSystem();
                ResponseEntity<PagedModel<EntityModel<Account>>> response = accountsClient.retrieveAccountList(new AccountSearchParameters(), 0, 1);
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

    private void updateUser(String email, Connection connection) {

        LOGGER.info("Updating projects for user {}", email);

        String error = "Unable to update user " + email;

        try {

            FeignSecurityManager.asSystem();

            ResponseEntity<Void> linkResponse = accountsClient.link(email, runtimeTenantResolver.getTenant());
            if (linkResponse == null || !linkResponse.getStatusCode().is2xxSuccessful()) {
                throw new FlywayException(error);
            }

            Account account = HateoasUtils.unwrap(accountsClient.retrieveAccounByEmail(email).getBody());
            updateUser(email, account.getFirstName(), account.getLastName(), connection);

        } catch (Exception e) {
            LOGGER.error(error, e);
            throw new FlywayException(error);
        } finally {
            FeignSecurityManager.reset();
        }

    }

    private void updateUser(String email, String firstName, String lastName, Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USERS)) {
            preparedStatement.setString(1, firstName);
            preparedStatement.setString(2, lastName);
            preparedStatement.setTimestamp(3, Timestamp.from(NOW));
            preparedStatement.setString(4, email);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }
    }

}
