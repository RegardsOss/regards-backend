package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.plugins.exception.RightsPluginConfigurationNotFoundException;
import fr.cnes.regards.modules.processing.utils.IPUserAuthFactory;
import io.vavr.collection.List;
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

import static fr.cnes.regards.framework.security.utils.HttpConstants.BEARER;

@Component
public class ProcessRepositoryImpl implements IPProcessRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRepositoryImpl.class);

    private final IPluginService pluginService;
    private final IWorkloadEngineRepository enginRepo;
    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;
    private final IRuntimeTenantResolver tenantResolver;
    private final IReactiveRolesClient rolesClient;
    private final IPUserAuthFactory authFactory;

    @Autowired
    public ProcessRepositoryImpl(IPluginService pluginService, IWorkloadEngineRepository enginRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo, IRuntimeTenantResolver tenantResolver,
            IReactiveRolesClient rolesClient, IPUserAuthFactory authFactory) {
        this.pluginService = pluginService;
        this.enginRepo = enginRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.tenantResolver = tenantResolver;
        this.rolesClient = rolesClient;
        this.authFactory = authFactory;
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
                .filter(p -> p.getBusinessId().equals(processId))
                .next();
    }

    @Override public Flux<PProcess> findAllByTenantAndUserRole(PUserAuth details) {
        return findRightsPluginConfigurations()
                .filterWhen(rights -> roleIsUnder(details, rights.getRole()))
                .flatMap(rights -> buildPProcess(details.getTenant(), rights));
    }

    @Override public Mono<PProcess> findByBatch(PBatch batch) {
        return findAllByTenant(batch.getTenant())
                .filter(process -> process.getBusinessId().equals(batch.getProcessBusinessId()))
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
        catch(ClassNotFoundException e) { return false; }
    }

    private Mono<Boolean> roleIsUnder(PUserAuth auth, String role) {
        // TODO add cache ; cache invalidation when an event on Roles is received?
        return rolesClient.shouldAccessToResourceRequiring(role, authHeader(auth));
    }

    private String authHeader(PUserAuth auth) {
        return BEARER + ": " + auth.getAuthToken();
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
                        return new PProcess(
                                UUID.fromString(pc.getBusinessId()),
                                pc.getLabel(),
                                pc.isActive(),
                                rpc.getTenant(),
                                rpc.getRole(),
                                List.ofAll(rpc.getDatasets()),
                                processDef.batchChecker(),
                                processDef.executionChecker(),
                                processDef.parameters(),
                                sizeForecast,
                                durationForecast,
                                engine,
                                processDef.executable()
                        );
                    })
                )
            )
        );
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

    private boolean withBusinessId(String processName, RightsPluginConfiguration rights) {
        return rights.getPluginConfiguration().getBusinessId().equals(processName);
    }

}
