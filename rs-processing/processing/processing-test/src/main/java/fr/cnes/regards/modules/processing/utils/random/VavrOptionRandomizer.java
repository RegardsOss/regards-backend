/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import io.vavr.control.Option;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;

/**
 * This class provides random generation for vavr Option.
 *
 * @author gandrieu
 */
public class VavrOptionRandomizer<T> extends AbstractRandomizer<Option<T>> {

    private Randomizer<Boolean> booleanRandomizer;

    private Randomizer<? extends T> valueRandomizer;

    public VavrOptionRandomizer(Randomizer<Boolean> booleanRandomizer, Randomizer<? extends T> valueRandomizer) {
        super();
        this.booleanRandomizer = booleanRandomizer;
        this.valueRandomizer = valueRandomizer;
    }

    public VavrOptionRandomizer() {
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

    @Override
    public Option<T> getRandomValue() {
        if (booleanRandomizer.getRandomValue()) {
            return Option.of(valueRandomizer.getRandomValue());
        } else {
            return Option.none();
        }
    }
}
