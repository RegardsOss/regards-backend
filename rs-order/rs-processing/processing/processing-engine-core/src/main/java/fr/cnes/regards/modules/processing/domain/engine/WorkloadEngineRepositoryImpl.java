package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import io.vavr.control.Option;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class WorkloadEngineRepositoryImpl implements IWorkloadEngineRepository {

    private final Map<String, IWorkloadEngine> enginesByName = new HashMap<>();

    @Override public Mono<IWorkloadEngine> register(IWorkloadEngine engine) {
        String name = engine.name();
        if (enginesByName.containsKey(name)) {
            return Mono.error(new EngineAlreadyExistsException(name));
        }
        else {
            return Mono.just(engine);
        }
    }

    @Override public Mono<IWorkloadEngine> findByName(String name) {
        return Mono.defer(() ->
            Option.of(enginesByName.get(name)).fold(
                    () -> Mono.error(new EngineNotFoundException(name)),
                    Mono::just
            )
        );
    }

    public static class EngineAlreadyExistsException extends Exception {
        public EngineAlreadyExistsException(String s) {
            super("Engine already exists for name '" + s + "'");
        }
    }

    public static class EngineNotFoundException extends Exception {
        public EngineNotFoundException(String s) {
            super("Engine not found for name '" + s + "'");
        }
    }
}
