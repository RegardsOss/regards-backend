/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;

/**
 * Integration tests for accounts global settings
 *
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@InstanceTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class AccountSettingsControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountSettingsControllerIT.class);

    /**
     * Business service to manage global accounts settings
     */
    @Autowired
    private IAccountSettingsService settingsService;

    /**
     *
     * Check that the system allows to retrieve global accounts settings.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the system allows to retrieve global accounts settings.")
    public void getSettings() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(AccountSettingsController.REQUEST_MAPPING_ROOT, expectations,
                          "Error retreiving accountsSettings");
    }

    /**
     *
     * Check that the system allows to update global accounts settings.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the system allows to update global accounts settings.")
    public void updateAccountSetting() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        final AccountSettings toUpdate = settingsService.retrieve();
        toUpdate.setMode("manual");
        performDefaultPut(AccountSettingsController.REQUEST_MAPPING_ROOT, toUpdate, expectations,
                          "Error updating accountsSettings to manual");

        expectations.clear();
        expectations.add(status().isOk());
        toUpdate.setMode("auto-accept");
        performDefaultPut(AccountSettingsController.REQUEST_MAPPING_ROOT, toUpdate, expectations,
                          "Error updating accountsSettings to auto-accept");

        expectations.clear();
        expectations.add(status().isUnprocessableEntity());
        toUpdate.setMode("sdfqjkmfsdq");
        performDefaultPut(AccountSettingsController.REQUEST_MAPPING_ROOT, toUpdate, expectations,
                          "There should be an error when updating accountsSettings to invalid value");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
