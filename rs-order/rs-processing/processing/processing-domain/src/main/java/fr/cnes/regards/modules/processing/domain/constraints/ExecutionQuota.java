package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.Function1;
import io.vavr.collection.List;
import org.apache.commons.io.FileUtils;

import static fr.cnes.regards.modules.processing.domain.constraints.IExecutionSingleBoundConstraint.BoundType.MAX;

public class ExecutionQuota<T extends Comparable<T>> implements IExecutionSingleBoundConstraint<T> {

    private final BoundType type;
    private final T bound;
    private final Function1<T, String> messageFormat;

    private ExecutionQuota(BoundType type, T bound, Function1<T, String> messageFormat) {
        this.type = type;
        this.bound = bound;
        this.messageFormat = messageFormat;
    }

    @Override public BoundType type() {
        return type;
    }

    @Override public T bound() {
        return bound;
    }

    @Override public ExecutionConstraintViolation violationFor(T actualValue) {
        return new ExecutionConstraintViolation(this, messageFormat.apply(actualValue));
    }

    public static <T extends Comparable<T>> ExecutionQuota<T> neverViolated() {
        return new ExecutionQuota<T>(null, null, t -> ""){
            @Override public boolean simpleCheck(T actualValue) { return true; }
            @Override public List<ExecutionConstraintViolation> check(T actualValue) { return List.empty(); }
        };
    }

    public static ExecutionQuota<Long> maxBytesInCache(Long bound) {
        return new ExecutionQuota<>(
                MAX, bound,
                (actual) -> String.format("Max size in cache for this process is {} but would now be {}",
                       FileUtils.byteCountToDisplaySize(bound),
                       FileUtils.byteCountToDisplaySize(actual)
                )
        );
    }

    public static ExecutionQuota<Integer> maxParallelExecutionsForUser(Integer bound) {
        return new ExecutionQuota<>(
                MAX, bound,
                (actual) -> String.format("Max parallel runs for this process is {} but would now be {}",
                      FileUtils.byteCountToDisplaySize(bound),
                      FileUtils.byteCountToDisplaySize(actual)
                )
        );
    }
}
