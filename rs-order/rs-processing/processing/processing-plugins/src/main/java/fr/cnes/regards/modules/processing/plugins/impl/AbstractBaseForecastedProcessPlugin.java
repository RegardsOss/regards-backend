package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.forecast.ForecastParser;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import io.vavr.control.Try;

/**
 * This process definition delegates the setting of the size/duration forecast to the admin.
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

    @Override public Try<IResultSizeForecast> sizeForecast() {
        return ForecastParser.INSTANCE.parseResultSizeForecast(sizeForecast);
    }

    @Override public Try<IRunningDurationForecast> durationForecast() {
        return ForecastParser.INSTANCE.parseRunningDurationForecast(durationForecast);
    }

    public void setSizeForecast(String sizeForecast) {
        this.sizeForecast = sizeForecast;
    }

    public void setDurationForecast(String durationForecast) {
        this.durationForecast = durationForecast;
    }
}
