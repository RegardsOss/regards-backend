package fr.cnes.regards.modules.processing.domain.duration;

import io.vavr.control.Try;

import java.time.Duration;

public interface IRunningDurationForecast {

    Duration expectedRunningDurationInBytes(long inputSizeInBytes);

    static IRunningDurationForecast defaultDuration() { return i -> Duration.ofDays(1); }

    interface Parser {
        Try<IRunningDurationForecast> parseRunningDurationForecast(String str);
    }
}
