package fr.cnes.regards.modules.processing.domain.size;

import lombok.Value;

@Value
public class AbsoluteResultSizeForecast implements IResultSizeForecast {

    long expectedSize;

    @Override public long expectedResultSizeInBytes(long inputSizeInBytes) {
        return expectedSize;
    }

}
