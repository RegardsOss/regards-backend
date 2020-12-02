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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IRoleCheckerService;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.plugins.exception.RightsPluginConfigurationNotFoundException;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static fr.cnes.regards.modules.processing.order.Constants.PROCESS_INFO_ROLE_PARAM_NAME;
import static fr.cnes.regards.modules.processing.order.Constants.PROCESS_INFO_TENANT_PARAM_NAME;

@Component
public class OrderProcessRepositoryImpl implements IPProcessRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessRepositoryImpl.class);

    private final IPluginService pluginService;
    private final IWorkloadEngineRepository enginRepo;
    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;
    private final IRuntimeTenantResolver tenantResolver;
    private final IRoleCheckerService roleChecker;

    @Autowired
    public OrderProcessRepositoryImpl(
            IPluginService pluginService,
            IWorkloadEngineRepository enginRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo,
            IRuntimeTenantResolver tenantResolver,
            IRoleCheckerService roleChecker
    ) {
        this.pluginService = pluginService;
        this.enginRepo = enginRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.tenantResolver = tenantResolver;
        this.roleChecker = roleChecker;
    }

    @Override public Flux<PProcess> findAllByTenant(String tenant) {
        return findRightsPluginConfigurations()
                .flatMap(rights -> buildPProcess(tenant, rights));
    }

    @Override public Mono<PProcess> findByTenantAndProcessName(String tenant, String processName) {
            return findAllByTenant(tenant)
                    .filter(p -> p.getProcessName().equals(processName))
                    .next();
    }

    @Override public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
        return findRightsPluginConfigurations()
                .flatMap(rights -> buildPProcess(tenant, rights))
                .filter(p -> p.getProcessId().equals(processId))
                .next();
    }

    @Override public Flux<PProcess> findAllByTenantAndUserRole(PUserAuth details) {
        return findRightsPluginConfigurations()
                .filterWhen(rights -> roleChecker.roleIsUnder(details, rights.getRole()))
                .flatMap(rights -> buildPProcess(details.getTenant(), rights));
    }

    @Override public Mono<PProcess> findByBatch(PBatch batch) {
        return findAllByTenant(batch.getTenant())
                .filter(process -> process.getProcessId().equals(batch.getProcessBusinessId()))
                .next();
    }

    private Mono<PProcess> buildPProcess(String tenant, RightsPluginConfiguration rights) {
        return getProcessDefinition(tenant, rights.getPluginConfiguration().getBusinessId())
                .flatMap(def -> fromPlugin(rights, def));
    }

    private Flux<PluginConfiguration> findPluginConfigurations() {
        return Flux.fromIterable(pluginService.getAllPluginConfigurations())
                .filter(this::eligibleClass);
    }

    private Flux<RightsPluginConfiguration> findRightsPluginConfigurations() {
        return findPluginConfigurations()
                .flatMap(this::getByPluginConfigurationId);
    }

    private Mono<RightsPluginConfiguration> getByPluginConfigurationId(PluginConfiguration pc) {
        return Mono.defer(() -> rightsPluginConfigRepo.findByPluginConfiguration(pc)
                .map(Mono::just)
                .getOrElse(() -> Mono.error(new RightsPluginConfigurationNotFoundException(pc))));
    }

    private boolean eligibleClass(PluginConfiguration pc) {
        try {
            String pluginClassName = PluginUtils.getPluginMetadata(pc.getPluginId()).getPluginClassName();
            return Class.forName(pluginClassName).isAssignableFrom(IProcessDefinition.class); }
        catch(Exception e) { return false; }
    }

    private <T> Mono<T> tryToMono(Try<T> t) {
        return t.fold(Mono::error, Mono::just);
    }

    public Mono<PProcess> fromPlugin(RightsPluginConfiguration rpc, IProcessDefinition processDef) {
        return Mono.defer(() -> tryToMono(processDef.sizeForecast())
            .flatMap(sizeForecast -> tryToMono(processDef.durationForecast())
                .flatMap(durationForecast -> enginRepo.findByName(processDef.engineName())
                    .map(engine -> {
                        PluginConfiguration pc = rpc.getPluginConfiguration();
                        return new PProcess.ConcretePProcess(
                                UUID.fromString(pc.getBusinessId()),
                                pc.getLabel(),
                                addTenantRole(processDef.processInfo(), rpc.getTenant(), rpc.getRole()),
                                pc.isActive(),
                                processDef.batchChecker(),
                                processDef.executionChecker(),
                                processDef.parameters(),
                                sizeForecast,
                                durationForecast,
                                engine,
                                processDef.executable(),
                                processDef.inputOutputMapper()
                        );
                    })
                )
            )
        );
    }

    private Map<String, String> addTenantRole(Map<String, String> processInfo, String tenant, String role) {
        return processInfo
            .put(PROCESS_INFO_TENANT_PARAM_NAME, tenant)
            .put(PROCESS_INFO_ROLE_PARAM_NAME, role);
    }

    private Mono<IProcessDefinition> getProcessDefinition(String tenant, String processName) {
        return fromOptional(() -> getOptionalPlugin(tenant, processName));
    }

    private Optional<IProcessDefinition> getOptionalPlugin(String tenant, String processName)
            throws NotAvailablePluginConfigurationException {
        tenantResolver.forceTenant(tenant);
        return pluginService.getOptionalPlugin(processName);
    }

    private <T> Mono<T> fromOptional(Callable<Optional<T>> copt) {
        return Mono.fromCallable(copt)
                .flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));
    }

}
