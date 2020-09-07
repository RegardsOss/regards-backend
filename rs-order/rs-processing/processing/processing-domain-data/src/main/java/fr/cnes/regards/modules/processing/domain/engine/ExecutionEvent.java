package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import lombok.Value;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.of;

public abstract class ExecutionEvent {

    @Value
    public static class FinalEvent extends ExecutionEvent {
        PStepFinal step;
        Seq<POutputFile> outputFiles;
        @Override public Option<PStep> step() { return of(step); }
        @Override public Seq<POutputFile> outputFiles() {
            return outputFiles;
        }
    }

    @Value
    public static class IntermediaryEvent extends ExecutionEvent {
        PStepIntermediary step;
        @Override public Option<PStep> step() { return of(step); }
        @Override public Seq<POutputFile> outputFiles() {
            return List.empty();
        }
    }

    @Value
    public static class OutputFileEvent extends ExecutionEvent {
        Seq<POutputFile> outputFiles;
        @Override public Option<PStep> step() { return none(); }
        @Override public Seq<POutputFile> outputFiles() {
            return outputFiles;
        }
    }


    public static FinalEvent event(PStepFinal step) {
        return new FinalEvent(step, List.empty());
    }
    public static FinalEvent event(PStepFinal step, Seq<POutputFile> files) {
        return new FinalEvent(step, files);
    }
    public static IntermediaryEvent event(PStepIntermediary step) {
        return new IntermediaryEvent(step);
    }
    public static OutputFileEvent event(Seq<POutputFile> files) {
        return new OutputFileEvent(files);
    }

    public abstract Option<PStep> step();

    public abstract Seq<POutputFile> outputFiles();

    public boolean isFinal() {
        return step().map(s -> s.getStatus().isFinalStep()).getOrElse(false);
    }
}

