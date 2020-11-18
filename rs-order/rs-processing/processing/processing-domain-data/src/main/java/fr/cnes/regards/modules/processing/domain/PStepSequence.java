package fr.cnes.regards.modules.processing.domain;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.Value;

@Value
public class PStepSequence {

    Seq<PStep> steps;

    public static PStepSequence empty() {
        return new PStepSequence(List.empty());
    }

    public static PStepSequence of(PStep... steps) {
        return new PStepSequence(List.of(steps));
    }

    public PStepSequence add(PStep step) {
        return new PStepSequence(this.steps.append(step));
    }

}
