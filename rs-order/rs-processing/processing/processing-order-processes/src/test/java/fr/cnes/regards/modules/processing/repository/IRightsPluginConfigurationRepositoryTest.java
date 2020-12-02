/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnableFeignClients(basePackageClasses = { IRolesClient.class, IStorageRestClient.class })
public class IRightsPluginConfigurationRepositoryTest extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRightsPluginConfigurationRepositoryTest.class);

    @Autowired IRightsPluginConfigurationRepository rightsRepo;

    @Before
    public void setup() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
    }


    @MultitenantTransactional
    @Test
    public void test_update_role() {

        PluginConfiguration pc = makeConfig();
        UUID processBusinessId = UUID.fromString(pc.getBusinessId());
        RightsPluginConfiguration rpc = new RightsPluginConfiguration(
                null,
                pc,
                processBusinessId,
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{},
                true
        );
        RightsPluginConfiguration persistedAllDatasetsArrayEmpty = rightsRepo.save(rpc);

        rightsRepo.updateRoleToForProcessBusinessId("ADMIN", processBusinessId);


        Optional<RightsPluginConfiguration> fetched = rightsRepo.findById(rpc.getId());
        assertThat(fetched.map(RightsPluginConfiguration::getRole)).contains("ADMIN");
    }

    @MultitenantTransactional
    @Test
    public void test_linked_to_all_datasets_with_empty_array() {
        String targetDataset = randomDataset();

        PluginConfiguration configAllDatasetsArrayEmpty = makeConfig();
        RightsPluginConfiguration allDatasetsArrayEmpty = new RightsPluginConfiguration(
                null,
                configAllDatasetsArrayEmpty,
                UUID.fromString(configAllDatasetsArrayEmpty.getBusinessId()),
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{},
                true
        );
        RightsPluginConfiguration persistedAllDatasetsArrayEmpty = rightsRepo.save(allDatasetsArrayEmpty);

        java.util.List<RightsPluginConfiguration> referencingTargetBeforeUpdate = rightsRepo.findByReferencedDataset(targetDataset);
        assertThat(referencingTargetBeforeUpdate).hasSize(1);
        assertThat(referencingTargetBeforeUpdate.get(0).getId()).isEqualTo(persistedAllDatasetsArrayEmpty.getId());
    }


    @MultitenantTransactional
    @Test
    public void test_crud_and_update() {
        String targetDataset = randomDataset();

        PluginConfiguration confOne = makeConfig();
        RightsPluginConfiguration rightsOne = new RightsPluginConfiguration(
                null,
                confOne,
                UUID.fromString(confOne.getBusinessId()),
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{targetDataset, randomDataset() },
                false
        );
        RightsPluginConfiguration persistedOne = rightsRepo.save(rightsOne);

        PluginConfiguration confTwo = makeConfig();
        RightsPluginConfiguration rightsTwo = new RightsPluginConfiguration(
                null,
                confTwo,
                UUID.fromString(confTwo.getBusinessId()),
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{ randomDataset(), randomDataset() },
                true
        );
        RightsPluginConfiguration persistedTwo = rightsRepo.save(rightsTwo);

        PluginConfiguration confThree = makeConfig();
        RightsPluginConfiguration rightsThree = new RightsPluginConfiguration(
                null,
                confThree,
                UUID.fromString(confThree.getBusinessId()),
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{ randomDataset(), randomDataset() },
                false
        );
        RightsPluginConfiguration persistedThree = rightsRepo.save(rightsThree);

        java.util.List<RightsPluginConfiguration> referencingTargetBeforeUpdate = rightsRepo.findByReferencedDataset(targetDataset);
        assertThat(referencingTargetBeforeUpdate).hasSize(2);
        assertThat(referencingTargetBeforeUpdate.get(0).getId()).isEqualTo(persistedOne.getId());
        assertThat(referencingTargetBeforeUpdate.get(1).getId()).isEqualTo(persistedTwo.getId());

        rightsRepo.updateAllAddDatasetOnlyForIds(List.of(UUID.fromString(confThree.getBusinessId())).toJavaList(), targetDataset);

        java.util.List<RightsPluginConfiguration> referencingTargetAfterUpdate = rightsRepo.findByReferencedDataset(targetDataset);
        assertThat(referencingTargetAfterUpdate).hasSize(2);
        assertThat(referencingTargetAfterUpdate.get(0).getId()).isEqualTo(persistedTwo.getId()); // Because applicable to all datasets, has not been changed
        assertThat(referencingTargetAfterUpdate.get(1).getId()).isEqualTo(persistedThree.getId());

    }

    private PluginConfiguration makeConfig() {
        PluginConfiguration conf = new PluginConfiguration("some_label", "some_plugin_ID");
        conf.setVersion("1.0.0");
        conf.setPriorityOrder(0);
        conf.setBusinessId(UUID.randomUUID().toString());
        return conf;
    }

    public String randomDataset() {
        return "URN:DATASET:tenant:" + UUID.randomUUID();
    }
}