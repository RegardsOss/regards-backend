package fr.cnes.regards.modules.processing.domain.duration;

import lombok.Value;

import java.time.Duration;

@Value
public class AbsoluteRunningDurationForecast implements IRunningDurationForecast {

    long milliseconds;

    @Override public Duration expectedRunningDurationInBytes(long inputSizeInBytes) {
        return Duration.ofMillis(milliseconds);
    }
}
