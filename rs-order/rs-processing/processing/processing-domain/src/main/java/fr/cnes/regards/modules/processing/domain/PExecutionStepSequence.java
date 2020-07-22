package fr.cnes.regards.modules.processing.domain;

import io.vavr.collection.Seq;
import lombok.Value;

@Value
public class PExecutionStepSequence {

    Seq<PExecutionStep> steps;

}
