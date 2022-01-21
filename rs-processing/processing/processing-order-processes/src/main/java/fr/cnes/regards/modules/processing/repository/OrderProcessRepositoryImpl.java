/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParameterUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
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
                .flatMap(rights -> buildPProcess(tenant, rights, HashMap.empty()));
        tenantResolver.clearTenant();
        return process;
    }

    @Override
    public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
        tenantResolver.forceTenant(tenant);
        Mono<PProcess> process = rightsPluginConfigRepo.findByPluginConfigurationBusinessId(processId.toString())
                .map(Mono::just).getOrElse(() -> Mono.error(new RuntimeException("Unfound " + processId)))
                .flatMap(rights -> buildPProcess(tenant, rights, HashMap.empty()));
        tenantResolver.clearTenant();
        return process;
    }

    @Override
    public Mono<PProcess> findByBatch(PBatch batch) {
        String tenant = batch.getTenant();
        UUID processId = batch.getProcessBusinessId();
        tenantResolver.forceTenant(tenant);
        Mono<PProcess> process = rightsPluginConfigRepo.findByPluginConfigurationBusinessId(processId.toString())
                .map(Mono::just).getOrElse(() -> Mono.error(new RuntimeException("Unfound " + processId)))
                .flatMap(rights -> buildPProcess(tenant, rights, batch.getUserSuppliedParameters().toMap(v -> Tuple.of(v.getName(), v.getValue()))));
        tenantResolver.clearTenant();
        return process;
    }

    private Mono<PProcess> buildPProcess(String tenant, RightsPluginConfiguration rights, Map<String,String> dParams) {
        return getProcessDefinition(tenant, rights.getPluginConfiguration().getBusinessId(), dParams)
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

    private Mono<IProcessDefinition> getProcessDefinition(String tenant, String processBusinessId, Map<String,String> dParams) {
        return fromOptional(() -> getOptionalPlugin(tenant, processBusinessId, dParams));
    }

    private Optional<IProcessDefinition> getOptionalPlugin(String tenant, String processBusinessId, Map<String,String> dParams)
            throws NotAvailablePluginConfigurationException {
        tenantResolver.forceTenant(tenant);
        try {
            if (dParams.isEmpty()) {
                return pluginService.getOptionalPlugin(processBusinessId);
            }
            else {
                PluginConfiguration pcWithParams = pluginService.getPluginConfiguration(processBusinessId);
                IPluginParam[] params = dParams.map((name, value) -> {
                    IPluginParam parameter = pcWithParams.getParameter(name);
                    return Tuple.of(name, PluginParameterUtils.forType(parameter.getType(), name, value, true));
                })
                .values()
                .toJavaArray(IPluginParam[]::new);
                return pluginService.getOptionalPlugin(processBusinessId, params);
            }
        }
        catch(EntityNotFoundException e) {
            return Optional.empty();
        }
        finally {
            tenantResolver.clearTenant();
        }
    }

    private <T> Mono<T> fromOptional(Callable<Optional<T>> copt) {
        return Mono.fromCallable(copt).flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));
    }

}
