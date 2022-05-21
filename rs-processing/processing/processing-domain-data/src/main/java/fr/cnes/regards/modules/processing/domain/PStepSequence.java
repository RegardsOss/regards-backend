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
package fr.cnes.regards.modules.processing.domain;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.Value;

/**
 * This class represents a sequence of steps.
 * <p>
 * A step sequence is immutable.
 *
 * @author gandrieu
 */
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
