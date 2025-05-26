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
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.slf4j.Logger;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

/**
 * Common ancestor class to flyway migration classes that need to communicate with
 * rs-admin-instance. Sub-classes must call {@link #checkAdminInstanceServiceCanBeAccessed()}
 * before sending REST requests to rs-admin-instance.
 *
 * @author Julien Canches
 */
public abstract class AbstractAdminInstanceDependentMigration extends BaseJavaMigration {

    private static final int RETRY_DELAY = 30;

    protected final IAccountsClient accountsClient;

    protected AbstractAdminInstanceDependentMigration(IAccountsClient accountsClient) {
        this.accountsClient = accountsClient;
    }

    protected abstract Logger getLogger();

    /**
     * Wait until rs-admin-instance is ready to process REST requests.
     */
    protected final void checkAdminInstanceServiceCanBeAccessed() throws InterruptedException {
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
                        getLogger().info("Successfully contacted rs-admin-instance");
                    }
                    break;
                }
            } catch (Exception e) {
                String error = "Unable to contact rs-admin-instance";
                getLogger().error(error, e);
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

}
