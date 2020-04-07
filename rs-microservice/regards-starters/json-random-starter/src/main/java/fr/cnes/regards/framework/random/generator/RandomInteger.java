/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random.generator;

import java.util.Random;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public class RandomInteger extends AbstractRandomGenerator<Integer> {

    private static Random random = new Random();

    public RandomInteger(FunctionDescriptor fd) {
        super(fd);
    }

    @Override
    public Integer random() {
        return random.nextInt();
    }

    public Integer random(Integer leftLimit, Integer rightLimit) {
        return leftLimit + (random.nextInt(rightLimit - leftLimit));
    }
}
