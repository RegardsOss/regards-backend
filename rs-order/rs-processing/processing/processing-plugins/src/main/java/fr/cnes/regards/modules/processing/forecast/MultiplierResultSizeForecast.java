package fr.cnes.regards.modules.processing.forecast;

import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import lombok.Value;

@Value
public class MultiplierResultSizeForecast implements IResultSizeForecast {

    double multiplier;

    @Override public long expectedResultSizeInBytes(long inputSizeInBytes) {
        return (long) (multiplier * inputSizeInBytes);
    }
}
