package fr.cnes.regards.modules.processing.domain.execution;

import io.vavr.collection.List;
import io.vavr.collection.Seq;

public enum ExecutionStatus {

    /** Final state, ended correctly */
    SUCCESS,

    /** Final state, explicitly failed. */
    FAILURE,

    /** Final state, has been cancelled by user or admin. */
    CANCELLED,

    /** Final state, did not provide any feedback after some time. */
    TIMED_OUT,

    CLEANUP(List.of(SUCCESS)),

    /** Launched. */
    RUNNING(List.of(CLEANUP, SUCCESS, FAILURE, TIMED_OUT, CANCELLED)),

    /** Execution is being prepared (copying input files to working directory, etc.). */
    PREPARE(List.of(RUNNING)),

    /** Initial state, registered but not yet launched. */
    REGISTERED(List.of(PREPARE, RUNNING, CANCELLED)),
    ;

    public static final ExecutionStatus[] VALUES = values();
    private final Seq<ExecutionStatus> nextStates;

    ExecutionStatus(Seq<ExecutionStatus> nextStates) {
        this.nextStates = nextStates;
    }
    ExecutionStatus() {
        this.nextStates = List.empty();
    }

    public static Seq<ExecutionStatus> nonFinalStatusList() {
        return List.of(VALUES).filter(s -> !s.isFinalStep());
    }

    public Seq<ExecutionStatus> getNextStates() {
        return nextStates;
    }

    public boolean isFinalStep() {
        return getNextStates().isEmpty();
    }
}
