package fr.cnes.regards.modules.processing.domain.forecast;

import io.vavr.control.Try;

import java.time.Duration;

public interface IRunningDurationForecast {

    Duration expectedRunningDurationInBytes(long inputSizeInBytes);

    static IRunningDurationForecast defaultDuration() { return i -> Duration.ofDays(1); }

    static IRunningDurationForecast secondsPerMegabytes(int secondsPerMegabyte) {
        return i -> Duration.ofSeconds(secondsPerMegabyte * (i / (1024L*1024L)));
    }

    static IRunningDurationForecast constant(int seconds) {
        return i -> Duration.ofSeconds(seconds);
    }

    interface Parser {
        Try<IRunningDurationForecast> parseRunningDurationForecast(String str);
    }
}
