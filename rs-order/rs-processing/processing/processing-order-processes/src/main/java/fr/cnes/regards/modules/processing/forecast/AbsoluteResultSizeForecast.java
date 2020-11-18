package fr.cnes.regards.modules.processing.forecast;

import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import lombok.Value;

@Value
public class AbsoluteResultSizeForecast implements IResultSizeForecast {

    long expectedSize;

    @Override public long expectedResultSizeInBytes(long inputSizeInBytes) {
        return expectedSize;
    }

}
