package fr.cnes.regards.modules.dam.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Profile("!test")
public class V1_7_0__GroupMigration extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1_7_0__GroupMigration.class);

    private static final int RETRY_DELAY = 30;

    private static final String SELECT_USERS = "SELECT name, users FROM t_access_group t JOIN ta_access_group_users ta ON t.id = ta.access_group_id";

    private static final String SELECT_PUBLIC_GROUPS = "SELECT name FROM t_access_group WHERE public = TRUE";

    private static final String GROUP_NAME_COLUMN = "name";

    private static final String USER_COLUMN = "users";

    private final IProjectUsersClient projectUsersClient;

    private final boolean ignoreGroupMigration;

    public V1_7_0__GroupMigration(IProjectUsersClient projectUsersClient,
                                  @Value("${regards.migration.ignore_group_migration:true}")
                                  boolean ignoreQuotaMigration) {
        this.projectUsersClient = projectUsersClient;
        this.ignoreGroupMigration = ignoreQuotaMigration;
    }

    @Override
    public void migrate(Context context) throws InterruptedException {
        if (ignoreGroupMigration) {
            LOGGER.info("Java migration of {} ignored by configuration", this.getClass().getSimpleName());
            // stop migrating group on new tenant as there is nothing to do
            return;
        }

        LOGGER.info("Starting Java migration {}", this.getClass().getSimpleName());

        Connection connection = context.getConnection();

        checkAdminServiceCanBeAccessed();

        Set<String> users = fetchUsers();
        Map<String, Set<String>> groupsByUser = getGroupsByUser(connection);
        Set<String> publicGroups = getPublicGroups(connection);

        LOGGER.info("Found {} users to update", users.size());

        users.forEach(email -> {
            Set<String> groups = new HashSet<>(publicGroups);
            if (groupsByUser.containsKey(email)) {
                groups.addAll(groupsByUser.get(email));
            }
            updateUser(email, groups);
        });

        LOGGER.info("Completed Java migration {}", this.getClass().getSimpleName());
    }

    private void updateUser(String email, Set<String> groups) {

        LOGGER.info("Updating groups for user {}", email);

        String error = "Unable to update user " + email;

        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Void> response = projectUsersClient.linkAccessGroups(email, new ArrayList<>(groups));
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                throw new FlywayException(error);
            }
        } catch (Exception e) {
            LOGGER.error(error, e);
            throw new FlywayException(error);
        } finally {
            FeignSecurityManager.reset();
        }

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

    private Set<String> getPublicGroups(Connection connection) {

        Set<String> publicGroups = new HashSet<>();

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SELECT_PUBLIC_GROUPS)) {
                while (resultSet.next()) {
                    publicGroups.add(resultSet.getString(GROUP_NAME_COLUMN));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }

        return publicGroups;
    }

    private Map<String, Set<String>> getGroupsByUser(Connection connection) {

        Map<String, Set<String>> groupsByUser = new HashMap<>();

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SELECT_USERS)) {
                while (resultSet.next()) {
                    String group = resultSet.getString(GROUP_NAME_COLUMN);
                    String user = resultSet.getString(USER_COLUMN);
                    if (groupsByUser.containsKey(user)) {
                        groupsByUser.get(user).add(group);
                    } else {
                        groupsByUser.put(user, new HashSet<>(Collections.singletonList(group)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }

        return groupsByUser;
    }

    private Set<String> fetchUsers() {
        try {
            FeignSecurityManager.asSystem();
            return HateoasUtils.retrieveAllPages(100,
                                                 pageable -> projectUsersClient.retrieveProjectUserList(new ProjectUserSearchParameters(),
                                                                                                        pageable))
                               .stream()
                               .map(ProjectUser::getEmail)
                               .collect(Collectors.toSet());
        } catch (Exception e) {
            String error = "Unable to contact rs-admin";
            LOGGER.error(error, e);
            throw new FlywayException(error);
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
