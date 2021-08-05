package fr.cnes.regards.modules.storage.service.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import io.vavr.collection.List;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
public class V1_7_0__QuotaMigration extends BaseJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1_7_0__QuotaMigration.class);

    private static final int RETRY_DELAY = 30;
    private static final String SELECT_USERS = "SELECT email, max_quota FROM t_user_download_quota_limits";
    private static final String EMAIL_COLUMN = "email";
    private static final String QUOTA_COLUMN = "max_quota";

    private final IProjectUsersClient projectUsersClient;

    public V1_7_0__QuotaMigration(IProjectUsersClient projectUsersClient) {
        this.projectUsersClient = projectUsersClient;
    }


    @Override
    public void migrate(Context context) throws InterruptedException {

        LOGGER.info("Starting Java migration {}", this.getClass().getSimpleName());

        Connection connection = context.getConnection();

        checkAdminServiceCanBeAccessed();

        Map<String, Long> maxQuotaByUser = getMaxQuotaByUser(connection);

        LOGGER.info("Found {} users to update", maxQuotaByUser.size());

        maxQuotaByUser.forEach(this::updateUser);

        LOGGER.info("Completed Java migration {}", this.getClass().getSimpleName());
    }

    private void checkAdminServiceCanBeAccessed() throws InterruptedException {

        int maxAttempts = 3;
        int attempt = 0;

        while (attempt++ < maxAttempts) {

            try {
                FeignSecurityManager.asSystem();
                ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response = projectUsersClient.retrieveProjectUserList(new ProjectUserSearchParameters(), 0, 1);
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
                throw new FlywayException(error);
            } finally {
                FeignSecurityManager.reset();
            }

            if (attempt < maxAttempts) {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            }
        }
    }

    private Map<String, Long> getMaxQuotaByUser(Connection connection) {

        Map<String, Long> maxQuotaByUser = new HashMap<>();

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SELECT_USERS)) {
                while (resultSet.next()) {
                    maxQuotaByUser.put(resultSet.getString(EMAIL_COLUMN), resultSet.getLong(QUOTA_COLUMN));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlywayException(e);
        }

        return maxQuotaByUser;
    }

    private void updateUser(String email, Long maxQuota) {

        LOGGER.info("Updating maxQuota for user {}", email);

        String error = "Unable to update user " + email;

        try {

            FeignSecurityManager.asSystem();

            ResponseEntity<EntityModel<ProjectUser>> userResponse = projectUsersClient.retrieveProjectUserByEmail(email);
            if (userResponse != null && userResponse.getStatusCode().is2xxSuccessful()) {
                ProjectUser projectUser = HateoasUtils.unwrap(userResponse.getBody());
                if (projectUser == null) {
                    throw new FlywayException(error);
                }
                projectUser.setMaxQuota(maxQuota);
                projectUser.setMetadata(List.ofAll(projectUser.getMetadata()).distinctBy(MetaData::getId).asJava());
                ResponseEntity<EntityModel<ProjectUser>> updateResponse = projectUsersClient.updateProjectUser(projectUser.getId(), projectUser);
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
