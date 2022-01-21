/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
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

/**
 * This class defines different kinds of events occurring during an execution.
 *
 * @author gandrieu
 */
public abstract class ExecutionEvent {

    /**
     * A final event can not be followed by another event, it contains a final step.
     */
    @Value
    public static class FinalEvent extends ExecutionEvent {
        PStepFinal step;
        Seq<POutputFile> outputFiles;
        @Override public Option<PStep> step() { return of(step); }
        @Override public Seq<POutputFile> outputFiles() {
            return outputFiles;
        }
    }

    /**
     * An intermediary step, which must be followed by at least one other event.
     */
    @Value
    public static class IntermediaryEvent extends ExecutionEvent {
        PStepIntermediary step;
        @Override public Option<PStep> step() { return of(step); }
        @Override public Seq<POutputFile> outputFiles() {
            return List.empty();
        }
    }

    /**
     * A step containing only output files, but no intermediary step, in
     * case the execution generates output files in several internal running steps.
     */
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

