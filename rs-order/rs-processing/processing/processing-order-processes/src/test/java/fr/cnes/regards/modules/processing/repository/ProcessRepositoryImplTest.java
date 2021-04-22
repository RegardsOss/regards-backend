/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnableFeignClients(basePackageClasses = { IRolesClient.class, IStorageRestClient.class })
public class ProcessRepositoryImplTest extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRepositoryImplTest.class);

    @Autowired IPProcessRepository processRepo;
    @Autowired IRightsPluginConfigurationRepository rpcRepo;
    @Autowired IPluginConfigurationRepository pluginConfRepo;
    @Autowired IWorkloadEngineRepository engineRepo;

    @Test public void batch_save_then_getOne_byId() {
        // GIVEN

        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        engineRepo.register(new IWorkloadEngine() {
            @Override public void selfRegisterInRepo() { }
            @Override public String name() { return "JOBS"; }
            @Override
            public Mono<PExecution> run(ExecutionContext context) {
                return Mono.empty(); // Won't be used
            }
        });

        UUID processBusinessId = UUID.randomUUID();
        String processName = "theLabel";

        PluginConfiguration pc = new PluginConfiguration(processName, "thePluginId");
        pc.setVersion("1.0.0-SNAPSHOT");
        pc.setPriorityOrder(0);
        pc.setBusinessId(processBusinessId.toString());

        Map<String, PluginMetaData> plugins = PluginUtils.getPlugins();
        LOGGER.info("plugins: {}", plugins);

        pc.setMetaData(plugins.get("UselessProcessPlugin"));

        RightsPluginConfiguration rpc = new RightsPluginConfiguration(null, pc, processBusinessId, "ADMIN", new String[]{}, true);
        rpcRepo.save(rpc);

        // WHEN
        PProcess process = processRepo.findByTenantAndProcessBusinessID(TENANT_PROJECTA, processBusinessId)
                .doOnError(t -> LOGGER.error(t.getMessage(), t))
                .block();

        // THEN
        LOGGER.info("Found process: {}", process);
        assertThat(process.getProcessName()).isEqualTo(processName);
        assertThat(process.getProcessId()).isEqualTo(processBusinessId);

    }

}