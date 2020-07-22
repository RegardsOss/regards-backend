package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.duration.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.size.IResultSizeForecast;
import io.vavr.control.Option;
import io.vavr.control.Try;

public interface IProcessForecastDefinition {

    Try<IResultSizeForecast> sizeForecast();

    Try<IRunningDurationForecast> durationForecast();

}
