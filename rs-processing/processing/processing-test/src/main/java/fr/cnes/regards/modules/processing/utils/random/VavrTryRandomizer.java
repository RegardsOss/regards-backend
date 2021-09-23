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

import io.vavr.control.Try;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

/**
 * This class provides random generation for vavr Try.
 *
 * @author gandrieu
 */
public class VavrTryRandomizer<T> extends AbstractRandomizer<Try<T>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends T> valueRandomizer;

    public VavrTryRandomizer(Randomizer<Boolean> booleanRandomizer,
            Randomizer<? extends T> valueRandomizer) {
        super();
        this.booleanRandomizer = booleanRandomizer;
        this.valueRandomizer = valueRandomizer;
    }

    public VavrTryRandomizer() {
    }

    public Randomizer<Boolean> getBooleanRandomizer() {
        return booleanRandomizer;
    }

    public void setBooleanRandomizer(Randomizer<Boolean> booleanRandomizer) {
        this.booleanRandomizer = booleanRandomizer;
    }

    public Randomizer<? extends T> getValueRandomizer() {
        return valueRandomizer;
    }

    public void setValueRandomizer(Randomizer<? extends T> valueRandomizer) {
        this.valueRandomizer = valueRandomizer;
    }

    @Override public Try<T> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Try.success(valueRandomizer.getRandomValue());
        } else {
            return Try.failure(new RandomTryCreationException());
        }
    }

    public static final class RandomTryCreationException extends Exception {}
}
