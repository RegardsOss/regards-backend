package fr.cnes.regards.modules.processing.entities.mapping;

import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionStepEntityRepository;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.entities.*;
import io.vavr.collection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

@Component
public class DomainEntityMapperImpl implements DomainEntityMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEntityMapperImpl.class);

    @Autowired IBatchEntityRepository batchEntityRepo;
    @Autowired IExecutionEntityRepository execEntityRepo;
    @Autowired IExecutionStepEntityRepository stepEntityRepo;

    @Override
    public BatchEntity toEntity(PBatch batch) {
        return new BatchEntity(
                batch.getId(),
                batch.getCorrelationId(),
                batch.getTenant(),
                batch.getUser(),
                batch.getUserRole(),
                batch.getProcessName(),
                new ParamValues(batch.getUserSuppliedParameters().toJavaList()),
                new FileStatsByDataset(batch.getFilesetsByDataset().toJavaMap()),
                batch.isPersisted()
        );
    }

    @Override
    public Mono<PBatch> toDomain(BatchEntity entity) {
        return Mono.fromCallable(() -> new PBatch(
                entity.getCorrelationId(),
                entity.getId(),
                entity.getProcess(),
                entity.getTenant(),
                entity.getUserName(),
                entity.getUserRole(),
                List.ofAll(entity.getParameters().getValues()),
                HashMap.ofAll(entity.getFilesets().getMap()),
                entity.isPersisted()
        ));
    }

    @Override
    public ExecutionEntity toEntity(PExecution exec) {
        return new ExecutionEntity(
                exec.getId(),
                exec.getBatchId(),
                new FileParameters(exec.getInputFiles().toJavaList()),
                exec.getExpectedDuration().toMillis(),
                exec.isPersisted()
        );
    }

    @Override
    public Mono<PExecution> toDomain(ExecutionEntity entity) {
        return Mono.fromCallable(() -> new PExecution(
             entity.getId(),
             entity.getBatchId(),
             Duration.ofMillis(entity.getTimeoutAfterMillis()),
             List.ofAll(entity.getFileParameters().getValues()),
             entity.isPersisted()));
    }

    public Mono<PExecutionStep> toDomain(ExecutionStepEntity entity) {
        return Mono.fromCallable(() -> new PExecutionStep(
            entity.getId(),
            entity.getExecutionId(),
            entity.getStatus(),
            entity.getTime(),
            entity.getMessage()
        ));
    }

    public ExecutionStepEntity toEntity(PExecutionStep step) {
        return new ExecutionStepEntity(
                step.getId(),
                step.getExecutionId(),
                step.getStatus(),
                step.getTime(),
                step.getMessage()
        );
    }

    public Seq<URL> mapInputUrls(String value) {
        return Stream.of(value.split("\n"))
                .foldLeft(List.empty(), (acc, str) -> {
                    try {
                        return acc.append(new URL(str));
                    } catch (MalformedURLException e) {
                        LOGGER.warn("Malformed input URL: {}", str);
                        return acc;
                    }
                });
    }

    public String mapInputUrls(Seq<URL> value) {
        return value.map(URL::toString).foldLeft("", (acc, u) -> acc + "\n" + u);
    }

    public <T> java.util.List<T> mapSeqToJavaList(Seq<T> seq) {
        return seq.toJavaList();
    }

    public <K,V> java.util.Map<K,V> mapMapToJavaMap(Map<K,V> map) {
        return map.toJavaMap();
    }


}
