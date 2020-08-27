package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.entity.*;
import io.vavr.collection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class DomainEntityMapperImpl implements DomainEntityMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEntityMapperImpl.class);

    @Override
    public BatchEntity toEntity(PBatch batch) {
        return new BatchEntity(
                batch.getId(),
                batch.getProcessBusinessId(),
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
                entity.getProcessBusinessId(),
                entity.getProcessName(),
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
                toEntity(exec.getSteps()),
                exec.getSteps().lastOption().map(PStep::getStatus).getOrNull(),
                exec.getTenant(),
                exec.getUserName(),
                exec.getProcessBusinessId(),
                exec.getProcessName(),
                exec.getCreated(),
                exec.getLastUpdated(),
                exec.getVersion(),
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
                toDomain(entity.getSteps()),
                entity.getTenant(),
                entity.getUserName(),
                entity.getProcessBusinessId(),
                entity.getProcessName(),
                entity.getCreated(),
                entity.getLastUpdated(),
                entity.getVersion(),
                entity.isPersisted()));
    }

    public Steps toEntity(Seq<PStep> domain) {
        return new Steps(domain.map(this::toEntity).toJavaList());
    }

    public Step toEntity(PStep step) {
        return new Step(
            step.getStatus(),
            step.getTime().toEpochSecond() * 1000L,
            step.getMessage()
        );
    }

    public List<PStep> toDomain(Steps entity) {
        return List.ofAll(entity.getValues()).map(this::toDomain);
    }

    public PStep toDomain(Step entity) {
        return new PStep(
            entity.getStatus(),
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(entity.getEpochTs()), ZoneId.of("UTC")),
            entity.getMessage()
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
