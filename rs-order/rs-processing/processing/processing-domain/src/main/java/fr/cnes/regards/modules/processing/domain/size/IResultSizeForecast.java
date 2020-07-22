package fr.cnes.regards.modules.processing.domain.size;

import io.vavr.control.Try;

public interface IResultSizeForecast {

    long expectedResultSizeInBytes(long inputSizeInBytes);

    static IResultSizeForecast zeroSize() { return i -> 0L; }

    interface Parser {
        Try<IResultSizeForecast> parseResultSizeForecast(String str);
    }
}
