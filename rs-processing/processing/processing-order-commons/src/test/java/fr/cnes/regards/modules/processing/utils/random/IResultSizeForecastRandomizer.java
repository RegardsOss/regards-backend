/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * TODO: IResultSizeForecastRandomizer description
 */
package fr.cnes.regards.modules.processing.utils.random;

import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.forecast.AbsoluteResultSizeForecast;
import fr.cnes.regards.modules.processing.forecast.MultiplierResultSizeForecast;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.api.Randomizer;

public class IResultSizeForecastRandomizer implements TypedRandomizer<IResultSizeForecast> {

    @Override
    public Class<IResultSizeForecast> type() {
        return IResultSizeForecast.class;
    }

    @Override
    public Randomizer<IResultSizeForecast> randomizer(EasyRandom generator) {
        return () -> generator.nextBoolean() ?
            new AbsoluteResultSizeForecast(Math.abs(generator.nextInt())) :
            new MultiplierResultSizeForecast(Math.abs((float) generator.nextInt(10000)));
    }
}
