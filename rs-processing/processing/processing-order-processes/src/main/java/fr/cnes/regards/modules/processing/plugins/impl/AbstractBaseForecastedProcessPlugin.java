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
package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.forecast.ForecastParser;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.control.Try;

/**
 * This process definition delegates the setting of the size/duration forecast to the admin.
 *
 * @author gandrieu
 */
public abstract class AbstractBaseForecastedProcessPlugin implements IProcessDefinition {

    @PluginParameter(
            name = "sizeForecast",
            label = "Size forecast",
            description = ForecastParser.SIZE_DESCRIPTION
    )
    protected String sizeForecast;

    @PluginParameter(
            name = "durationForecast",
            label = "Duration forecast",
            description = ForecastParser.DURATION_DESCRIPTION
    )
    protected String durationForecast;

    @Override
    public Try<IResultSizeForecast> sizeForecast() {
        return ForecastParser.INSTANCE.parseResultSizeForecast(sizeForecast);
    }

    @Override
    public Try<IRunningDurationForecast> durationForecast() {
        return ForecastParser.INSTANCE.parseRunningDurationForecast(durationForecast);
    }

    public void setSizeForecast(String sizeForecast) {
        this.sizeForecast = sizeForecast;
    }

    public void setDurationForecast(String durationForecast) {
        this.durationForecast = durationForecast;
    }
}
