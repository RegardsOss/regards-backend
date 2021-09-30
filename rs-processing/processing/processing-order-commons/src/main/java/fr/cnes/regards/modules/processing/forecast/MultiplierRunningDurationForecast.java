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
package fr.cnes.regards.modules.processing.forecast;

import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import lombok.Value;

import java.time.Duration;
/**
 * This class represents a duration forecast depending on the input.
 *
 * @author gandrieu
 */
@Value
public class MultiplierRunningDurationForecast implements IRunningDurationForecast {

    double millisPerByte;

    @Override public Duration expectedRunningDurationInBytes(long inputSizeInBytes) {
        return Duration.ofMillis((long)(millisPerByte * inputSizeInBytes));
    }
}