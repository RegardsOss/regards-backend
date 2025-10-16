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
import fr.cnes.regards.framework.jpa.utils.RegardsJavaMigration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
@SuppressWarnings("java:S101") // Naming using underscores is required by flyway
public class V2_2_0__UnverifiedEmailsMigration extends BaseJavaMigration implements RegardsJavaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(V2_2_0__UnverifiedEmailsMigration.class);

    private static final String EMAIL_COLUMN = "email";

    /**
     * Main update query. Update the project users whose status is WAITING_EMAIL_VERIFICATION and that have
     * a corresponding token in table t_email_verification_token. Users status is updated to WAITING_ACCOUNT_ACTIVE
     * and all details, including the existing token, are returned.
     */
    private static final String UPDATE = "UPDATE t_project_user u SET status = 'WAITING_ACCOUNT_ACTIVE' "
                                         + "FROM t_email_verification_token t "
                                         + "WHERE t.project_user_id = u.id "
                                         + "AND u.status = 'WAITING_EMAIL_VERIFICATION' "
                                         + "RETURNING u.email, t.expiry_date, t.origin_url, t.request_link, t.token";

    /**
     * "Fixup" update query that update users whose status is WAITING_EMAIL_VERIFICATION but that have no corresponding
     * token in table t_email_verification_token. This is not a normal case since a user with this status should have
     * a verification token. However, in order not break the microservice, we must ensure that the status is always
     * updated even in this abnormal case.
     */
    private static final String FIXUP_UPDATE = "UPDATE t_project_user SET status = 'WAITING_ACCOUNT_ACTIVE' "
                                               + "WHERE status = 'WAITING_EMAIL_VERIFICATION' "
                                               + "RETURNING email";

    private static final String DROP_TOKENS_TABLE = "DROP TABLE IF EXISTS t_email_verification_token;";

    private static final String DROP_TOKENS_SEQUENCE = "DROP SEQUENCE IF EXISTS seq_email_verification_token;";

    private final IAccountsClient accountsClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public V2_2_0__UnverifiedEmailsMigration(IAccountsClient accountsClient,
                                             IRuntimeTenantResolver runtimeTenantResolver) {
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public String getModuleName() {
        return "accessrights"; // same string as in the scripts.accessrights folder that contain SQL migration files
    }

    private static UserAndToken translateRow(ResultSet rs) throws SQLException {
        return new UserAndToken(rs.getString(EMAIL_COLUMN),
                                new EmailVerificationTokenDto(rs.getTimestamp("expiry_date").toLocalDateTime(),
                                                              rs.getString("origin_url"),
                                                              rs.getString("request_link"),
                                                              rs.getString("token")));
    }

    @Override
    public void migrate(Context context) {
        // There's one migration per tenant, so we must include the tenant name in all messages/exceptions.
        String tenant = runtimeTenantResolver.getTenant();
        LOGGER.info("Starting Java migration {} for project {}", this.getClass().getSimpleName(), tenant);
        Connection connection = context.getConnection();
        // Update all users status from WAITING_EMAIL_VERIFICATION to WAITING_ACCOUNT_ACTIVE and get their token
        Set<UserAndToken> users = updateAndGetUnverifiedUsers(connection, tenant);
        LOGGER.info("Found {} unverified users to update in project {}", users.size(), tenant);
        // If some users are in WAITING_EMAIL_VERIFICATION but don't have a token, move their status anyway and warn
        // that it is a problem
        updateBrokenUnverifiedUsersAndWarn(connection, tenant);
        // For each user that we could get the token of, reset their account to EMAIL_VERIFICATION state. This involves
        // calling rs-admin-instance. Because rs-admin bootstrap has an explicit wait for rs-admin-instance
        // (runner.microservices.to.wait=rs-admin-instance), we are guaranteed at this point that rs-admin-instance is
        // ready.
        users.forEach(this::resetAccount);
        // Last, drop the no longer used table t_email_verification_token
        deleteEmailVerificationTokenTable(connection, tenant);
        LOGGER.info("Completed Java migration {} for project {}", this.getClass().getSimpleName(), tenant);
    }

    private Set<UserAndToken> updateAndGetUnverifiedUsers(Connection connection, String tenant) {
        Set<UserAndToken> users = new HashSet<>();
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(UPDATE)) {
            while (resultSet.next()) {
                users.add(translateRow(resultSet));
            }
        } catch (SQLException e) {
            throw new FlywayException(String.format("Unable to get unverified users list for project %s", tenant), e);
        }
        return users;
    }

    private void updateBrokenUnverifiedUsersAndWarn(Connection connection, String tenant) {
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(FIXUP_UPDATE)) {
            List<String> emails = new ArrayList<>();
            while (resultSet.next()) {
                emails.add(resultSet.getString(EMAIL_COLUMN));
            }
            if (!emails.isEmpty() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("The following users are in WAITING_EMAIL_VERIFICATION state but no verification token "
                            + "exists for them: {}. They have been moved to WAITING_ACCOUNT_ACTIVE state, and need to "
                            + "contact an instance administrator to get a new verification email.",
                            String.join(", ", emails));
            }
        } catch (SQLException e) {
            throw new FlywayException(String.format("Unable to get unverified users list with no verification token "
                                                    + "for project %s", tenant), e);
        }
    }

    private void deleteEmailVerificationTokenTable(Connection connection, String tenant) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(DROP_TOKENS_TABLE);
            statement.executeUpdate(DROP_TOKENS_SEQUENCE);
        } catch (SQLException e) {
            throw new FlywayException(String.format("Unable to delete table t_email_verification_token for project "
                                                    + "%s", tenant), e);
        }
    }

    @SuppressWarnings("java:S2221") // Difficult to catch a specific exception when calling feign
    private void resetAccount(UserAndToken user) {
        LOGGER.info("Resetting account {} to EMAIL_VERIFICATION", user.email());
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Void> linkResponse = accountsClient.resetEmailVerificationStatus(user.email(), user.token());
            if (linkResponse == null) {
                throw new FlywayException("No response from resetEmailVerificationStatus endPoint");
            }
            // Don't bother checking the status code because feign throws an exception if it is not 2xx
        } catch (HttpClientErrorException e) {
            // Conflict (409) is OK. It just means that at least two projects have the same user in
            // WAITING_EMAIL_VERIFICATION state. This has to be translated to the corresponding account only once.
            if (!e.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
                throw new FlywayException(String.format("Unexpected response %s from resetEmailVerificationStatus "
                                                        + "endPoint for user %s", e.getStatusCode(), user.email));
            }
        } catch (Exception e) {
            throw new FlywayException("Unable to update status on account " + user.email(), e);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @SuppressWarnings("java:S1186") // Basic record with no additional methods
    private record UserAndToken(String email,
                                EmailVerificationTokenDto token) {

    }

}
