package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import io.vavr.control.Try;

public interface IProcessForecastDefinition {

    Try<IResultSizeForecast> sizeForecast();

    Try<IRunningDurationForecast> durationForecast();

}
