package fr.cnes.regards.modules.processing.plugins.repository;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionQuota;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionRights;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.Callable;

@Component
public class ProcessRepositoryImpl implements IPProcessRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRepositoryImpl.class);

    private final IPluginService pluginService;
    private final IWorkloadEngineRepository enginRepo;

    @Autowired
    public ProcessRepositoryImpl(IPluginService pluginService, IWorkloadEngineRepository enginRepo) {
        this.pluginService = pluginService;
        this.enginRepo = enginRepo;
    }

    @Override public Mono<PProcess> findByName(String name) {
            return fromOptional(() -> pluginService.<IProcessDefinition>getOptionalPlugin(name))
                    .switchIfEmpty(Mono.defer(() -> Mono.error(new EntityNotFoundException(name))))
                    .flatMap(this::fromPlugin);
    }

    private <T> Mono<T> fromOptional(Callable<Optional<T>> copt) {
        return Mono.fromCallable(copt)
            .flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override public Flux<PProcess> findAll() {
        return Flux.fromIterable(pluginService.getAllPluginConfigurations())
                .filter(pc -> eligibleClass(pc))
                .map(PluginConfiguration::getBusinessId)
                .flatMap(this::findByName);
    }

    private boolean eligibleClass(PluginConfiguration pc) {
        try { return Class.forName(pc.getPluginClassName()).isAssignableFrom(IProcessDefinition.class); }
        catch(ClassNotFoundException e) { return false; }
    }

    @Override public Flux<PProcess> findAllByTenantAndUserRole(String tenant, String userRole) {
        return findAll()
            .filter(process -> process.getAllowedTenants().simpleCheck(tenant))
            .filter(process -> process.getAllowedUsersRoles().simpleCheck(userRole));
    }

    private <T> Mono<T> tryToMono(Try<T> t) {
        return t.fold(Mono::error, Mono::just);
    }

    public Mono<PProcess> fromPlugin(IProcessDefinition plugin) {
        return Mono.defer(() -> tryToMono(plugin.sizeForecast())
            .flatMap(sizeForecast -> tryToMono(plugin.durationForecast())
                .flatMap(durationForecast -> enginRepo.findByName(plugin.engineName())
                    .map(engine -> new PProcess(
                        plugin.processName(),
                        concQuota(plugin),
                        bytesQuota(plugin),
                        ExecutionRights.allowedUserRoles(plugin.allowedUserRoles()),
                        ExecutionRights.allowedDatasets(plugin.allowedDatasets()),
                        ExecutionRights.allowedTenants(plugin.allowedTenants()),
                        plugin.parameters(),
                        sizeForecast,
                        durationForecast,
                        engine,
                        plugin.executable()
                    ))
                )
            )
        );
    }

    private ExecutionQuota<Long> bytesQuota(IProcessDefinition plugin) {
        Option<Long> optBytes = plugin.maxBytesInCache();
        Option<ExecutionQuota<Long>> map = optBytes.map(x -> ExecutionQuota.maxBytesInCache(x));
        return map.getOrElse(ExecutionQuota::neverViolated);
    }

    private ExecutionQuota<Integer> concQuota(IProcessDefinition plugin) {
        Option<Integer> optMaxExec = plugin.maxConcurrentExecutions();
        Option<ExecutionQuota<Integer>> map = optMaxExec.map(x -> ExecutionQuota.maxParallelExecutionsForUser(x));
        return map.getOrElse(ExecutionQuota::neverViolated);
    }

}
