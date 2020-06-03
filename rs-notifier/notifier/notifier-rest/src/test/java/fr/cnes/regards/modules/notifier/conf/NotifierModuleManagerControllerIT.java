/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.conf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.microservice.rest.ModuleManagerController;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.notifier.dao.IRuleRepository;
import fr.cnes.regards.modules.notifier.domain.Rule;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notifier_it" })
public class NotifierModuleManagerControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRuleRepository ruleRepo;

    @Test
    public void importConfiguration() {
        Path filePath = Paths.get("src", "test", "resources", "rs-notifier.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");

        // Check
        try {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            Set<Rule> rules = ruleRepo.findByRulePluginActiveTrue();
            Assert.assertTrue(rules.size() == 1);
        } finally {
            runtimeTenantResolver.clearTenant();
        }

        // Import same configuration resetting existing import configuration without conflict
        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");
    }

    @Test
    public void importConfigurationWithInactiveRule() {
        Path filePath = Paths.get("src", "test", "resources", "rs-notifier-inactive.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");

        // Check
        try {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            Set<Rule> rules = ruleRepo.findByRulePluginActiveTrue();
            Assert.assertTrue(rules.isEmpty());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Test
    public void importConfigurationForDisableExistingRule() {
        this.importConfiguration();
        Path filePath = Paths.get("src", "test", "resources", "rs-notifier-disable-rule.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");

        // Check
        try {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            Set<Rule> rules = ruleRepo.findByRulePluginActiveTrue();
            Assert.assertTrue(rules.isEmpty());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
