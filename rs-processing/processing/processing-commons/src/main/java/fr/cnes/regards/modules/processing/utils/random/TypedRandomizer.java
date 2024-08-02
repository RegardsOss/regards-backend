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

import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

/**
 * This interface is meant to be used BY TESTS, and is a target for ServiceLoader, so that other components can
 * independently declare new randomizers to be loaded by RandomUtils in the processing-test module. It is just easier to
 * declare it here in order to prevent circular dependencies in maven modules.<br/>
 * Classes implementing this interface must be declared into META-INF/services/fr.cnes.regards.modules.processing.utils.random.TypedRandomizer
 *
 * @param <T> the randomized type
 * @author gandrieu
 */
public interface TypedRandomizer<T> {

    Class<T> type();

    Randomizer<T> randomizer(EasyRandom generator);

}
