/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.accessrights.service.migrations;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * This migration processes all ProjectUsers whose status is WAITING_EMAIL_VERIFICATION. This
 * status was removed in v2.2.0 and replaced with a similar status on the associated Account
 * in the admin instance microservice.
 * <p>
 * For each user in this case, their status is moved to WAITING_ACCOUNT_ACTIVE, and the REST API
 * /accounts/{email}/verification/reset is called to set the account status to EMAIL_VERIFICATION,
 * and transfer the email verification token.
 */
@Component
@Profile("!test")
public class V2_2_0__UnverifiedEmailsMigration extends AbstractAdminInstanceDependentMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V2_2_0__UnverifiedEmailsMigration.class);

    private static final String EMAIL_COLUMN = "email";

    private static final String UPDATE = "UPDATE t_project_user u SET status = 'WAITING_ACCOUNT_ACTIVE' "
                                         + "FROM t_email_verification_token t "
                                         + "WHERE t.project_user_id = u.id "
                                         + "AND u.status = 'WAITING_EMAIL_VERIFICATION' "
                                         + "RETURNING u.email, t.expiry_date, t.origin_url, t.request_link, t.token";

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public V2_2_0__UnverifiedEmailsMigration(IAccountsClient accountsClient,
                                             IRuntimeTenantResolver runtimeTenantResolver) {
        super(accountsClient);
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    private static UserAndToken translateRow(ResultSet rs) throws SQLException {
        return new UserAndToken(rs.getString(EMAIL_COLUMN),
                                new EmailVerificationTokenDto(rs.getTimestamp("expiry_date").toLocalDateTime(),
                                                              rs.getString("origin_url"),
                                                              rs.getString("request_link"),
                                                              rs.getString("token")));
    }

    @Override
    public void migrate(Context context) throws InterruptedException {
        String tenant = runtimeTenantResolver.getTenant();
        LOGGER.info("Starting Java migration {} for project {}", this.getClass().getSimpleName(), tenant);
        Connection connection = context.getConnection();
        checkAdminInstanceServiceCanBeAccessed();
        Set<UserAndToken> users = updateAndGetUnverifiedUsers(connection);
        LOGGER.info("Found {} unverified users to update in project {}", users.size(), tenant);
        users.forEach(this::resetAccount);
        LOGGER.info("Completed Java migration {} for project {}", this.getClass().getSimpleName(), tenant);
    }

    private Set<UserAndToken> updateAndGetUnverifiedUsers(Connection connection) {
        Set<UserAndToken> users = new HashSet<>();
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(UPDATE)) {
            while (resultSet.next()) {
                users.add(translateRow(resultSet));
            }
        } catch (SQLException e) {
            throw new FlywayException("Unable to get unverified users list", e);
        }
        return users;
    }

    private void resetAccount(UserAndToken user) {
        LOGGER.info("Resetting account {} to EMAIL_VERIFICATION", user.email());
        String error = "Unable to update status on account " + user.email();

        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Void> linkResponse = accountsClient.resetEmailVerificationStatus(user.email(), user.token());
            if (linkResponse == null || !linkResponse.getStatusCode().is2xxSuccessful()) {
                throw new FlywayException(error + ": " + linkResponse);
            }
        } catch (Exception e) {
            throw new FlywayException(error, e);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private record UserAndToken(String email,
                                EmailVerificationTokenDto token) {

    }

}
