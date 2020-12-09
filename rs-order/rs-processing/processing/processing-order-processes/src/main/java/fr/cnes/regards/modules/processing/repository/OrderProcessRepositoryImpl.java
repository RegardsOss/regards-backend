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

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static fr.cnes.regards.modules.processing.order.Constants.PROCESS_INFO_ROLE_PARAM_NAME;
import static fr.cnes.regards.modules.processing.order.Constants.PROCESS_INFO_TENANT_PARAM_NAME;

/**
 * This class is a concrete implementation of process repository based on {@link PluginConfiguration}.
 *
 * @author gandrieu
 */
@Component
public class OrderProcessRepositoryImpl implements IPProcessRepository {

    private final IPluginService pluginService;

    private final IWorkloadEngineRepository enginRepo;

    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;

    private final IRuntimeTenantResolver tenantResolver;

    @Autowired
    public OrderProcessRepositoryImpl(IPluginService pluginService, IWorkloadEngineRepository enginRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo, IRuntimeTenantResolver tenantResolver) {
        this.pluginService = pluginService;
        this.enginRepo = enginRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public Flux<PProcess> findAllByTenant(String tenant) {
        tenantResolver.forceTenant(tenant);
        Flux<PProcess> process = Flux.fromIterable(rightsPluginConfigRepo.findAll())
                .flatMap(rights -> buildPProcess(tenant, rights));
        tenantResolver.clearTenant();
        return process;
    }

    @Override
    public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
        tenantResolver.forceTenant(tenant);
        Mono<PProcess> process = rightsPluginConfigRepo.findByPluginConfigurationBusinessId(processId.toString())
                .map(Mono::just).getOrElse(() -> Mono.error(new RuntimeException("Unfound " + processId)))
                .flatMap(rights -> buildPProcess(tenant, rights));
        tenantResolver.clearTenant();
        return process;
    }

    @Override
    public Mono<PProcess> findByBatch(PBatch batch) {
        return findByTenantAndProcessBusinessID(batch.getTenant(), batch.getProcessBusinessId());
    }

    private Mono<PProcess> buildPProcess(String tenant, RightsPluginConfiguration rights) {
        return getProcessDefinition(tenant, rights.getPluginConfiguration().getBusinessId())
                .flatMap(def -> fromPlugin(rights, def, tenant));
    }

    private <T> Mono<T> tryToMono(Try<T> t) {
        return t.fold(Mono::error, Mono::just);
    }

    @VisibleForTesting
    public Mono<PProcess> fromPlugin(RightsPluginConfiguration rpc, IProcessDefinition processDef, String tenant) {
        OrderProcessInfoMapper mapper = new OrderProcessInfoMapper();
        return tryToMono(processDef.sizeForecast()).flatMap(sizeForecast -> tryToMono(processDef.durationForecast())
                .flatMap(durationForecast -> enginRepo.findByName(processDef.engineName()).map(engine -> {
                    PluginConfiguration pc = rpc.getPluginConfiguration();
                    return new PProcess.ConcretePProcess(UUID.fromString(pc.getBusinessId()), pc.getLabel(),
                            addTenantRole(mapper.toMap(processDef.processInfo()), tenant, rpc.getRole()), pc.isActive(),
                            processDef.batchChecker(), processDef.executionChecker(), processDef.parameters(),
                            sizeForecast, durationForecast, engine, processDef.executable(),
                            processDef.inputOutputMapper());
                })));
    }

    private Map<String, String> addTenantRole(Map<String, String> processInfo, String tenant, String role) {
        return processInfo.put(PROCESS_INFO_TENANT_PARAM_NAME, tenant).put(PROCESS_INFO_ROLE_PARAM_NAME, role);
    }

    private Mono<IProcessDefinition> getProcessDefinition(String tenant, String processName) {
        return fromOptional(() -> getOptionalPlugin(tenant, processName));
    }

    private Optional<IProcessDefinition> getOptionalPlugin(String tenant, String processName)
            throws NotAvailablePluginConfigurationException {
        tenantResolver.forceTenant(tenant);
        Optional<IProcessDefinition> process = pluginService.getOptionalPlugin(processName);
        tenantResolver.clearTenant();
        return process;
    }

    private <T> Mono<T> fromOptional(Callable<Optional<T>> copt) {
        return Mono.fromCallable(copt).flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));
    }

}
