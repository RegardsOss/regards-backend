package fr.cnes.regards.modules.processing.domain.duration;

import lombok.Value;

import java.time.Duration;

@Value
public class MultiplierRunningDurationForecast implements IRunningDurationForecast {

    double millisPerByte;

    @Override public Duration expectedRunningDurationInBytes(long inputSizeInBytes) {
        return Duration.ofMillis((long)(millisPerByte * inputSizeInBytes));
    }
}
