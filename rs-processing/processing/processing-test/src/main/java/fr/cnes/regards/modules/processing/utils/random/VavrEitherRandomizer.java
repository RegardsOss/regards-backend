/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.random;

import io.vavr.control.Either;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

/**
 * This class provides random generation for vavr Either.
 *
 * @author gandrieu
 */
public class VavrEitherRandomizer<L, R> extends AbstractRandomizer<Either<L, R>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends R> rightRandomizer;

    private Randomizer<? extends L> leftRandomizer;

    public VavrEitherRandomizer() {
    }

    public VavrEitherRandomizer(Randomizer<Boolean> booleanRandomizer,
            Randomizer<? extends R> rightRandomizer, Randomizer<? extends L> leftRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
        this.rightRandomizer = rightRandomizer;
        this.leftRandomizer = leftRandomizer;
    }

    public Randomizer<Boolean> getBooleanRandomizer() {
        return booleanRandomizer;
    }

    public Randomizer<? extends R> getRightRandomizer() {
        return rightRandomizer;
    }

    public Randomizer<? extends L> getLeftRandomizer() {
        return leftRandomizer;
    }

    public void setBooleanRandomizer(Randomizer<Boolean> booleanRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
    }

    public void setRightRandomizer(Randomizer<? extends R> rightRandomizer) {
        this.rightRandomizer = rightRandomizer;
    }

    public void setLeftRandomizer(Randomizer<? extends L> leftRandomizer) {
        this.leftRandomizer = leftRandomizer;
    }

    @Override public Either<L, R> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Either.right(rightRandomizer.getRandomValue());
        } else {
            return Either.left(leftRandomizer.getRandomValue());
        }
    }
}
