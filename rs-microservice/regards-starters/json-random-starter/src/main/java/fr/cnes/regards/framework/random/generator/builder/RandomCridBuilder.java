/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random.generator.builder;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.generator.AbstractRandomGenerator;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Locale;
import java.util.Random;

/**
 * Remove this SWOT specific builder
 */
@Deprecated
public class RandomCridBuilder implements RandomGeneratorBuilder<RandomCridBuilder.RandomCrid> {

    private static final Random RANDOM_GEN = new Random();

    @Override
    public String getFunctionName() {
        return "crid";
    }

    @Override
    public RandomCrid build(FunctionDescriptor fd) {
        return new RandomCrid(fd);
    }

    public static class RandomCrid extends AbstractRandomGenerator<String> {

        public RandomCrid(FunctionDescriptor fd) {
            super(fd);
        }

        @Override
        public String random() {

            String firstPool = "DPTVX";
            String secondPool = "GIO";

            return generateCharFromPool(firstPool) + generateCharFromPool(secondPool) + RandomStringUtils
                    .randomAlphanumeric(2).toUpperCase(Locale.ROOT);
        }

        /**
         * Create a random string from a character pool
         *
         * @param pool characters pool
         * @return random string
         */
        String generateCharFromPool(String pool) {

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                int randomInt = RANDOM_GEN.nextInt(pool.length());
                builder.append(pool.charAt(randomInt));
            }
            return builder.toString();
        }
    }
}
