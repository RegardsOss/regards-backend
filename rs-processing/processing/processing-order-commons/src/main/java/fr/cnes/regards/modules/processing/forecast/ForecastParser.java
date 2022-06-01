/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.forecast;

import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Predicates.not;

/**
 * This class is a parser for forecasts.
 * <p>
 * See {@link #SIZE_DESCRIPTION} and {@link #DURATION_DESCRIPTION} for details.
 *
 * @author gandrieu
 */
public class ForecastParser implements IResultSizeForecast.Parser, IRunningDurationForecast.Parser {

    public static final ForecastParser INSTANCE = new ForecastParser();

    private ForecastParser() {
    }

    /*=================== Useful constants ===================*/

    static final long BYTES_IN_ONE_BYTE = 1L;

    static final long BYTES_IN_ONE_KILOBYTE = 1024L;

    static final long BYTES_IN_ONE_MEGABYTE = 1024L * BYTES_IN_ONE_KILOBYTE;

    static final long BYTES_IN_ONE_GIGABYTE = 1024L * BYTES_IN_ONE_MEGABYTE;

    static final long MILLIS_IN_ONE_SECOND = 1000L;

    static final long MILLIS_IN_ONE_MINUTE = 60 * MILLIS_IN_ONE_SECOND;

    static final long MILLIS_IN_ONE_HOUR = 60 * MILLIS_IN_ONE_MINUTE;

    static final long MILLIS_IN_ONE_DAY = 24 * MILLIS_IN_ONE_HOUR;

    private static final Map<String, Long> bytesPerUnit = HashMap.of("b",
                                                                     BYTES_IN_ONE_BYTE,
                                                                     "k",
                                                                     BYTES_IN_ONE_KILOBYTE,
                                                                     "m",
                                                                     BYTES_IN_ONE_MEGABYTE,
                                                                     "g",
                                                                     BYTES_IN_ONE_GIGABYTE);

    private static final Map<String, Long> millisPerUnit = HashMap.of("s",
                                                                      MILLIS_IN_ONE_SECOND,
                                                                      "min",
                                                                      MILLIS_IN_ONE_MINUTE,
                                                                      "h",
                                                                      MILLIS_IN_ONE_HOUR,
                                                                      "d",
                                                                      MILLIS_IN_ONE_DAY);

    /*=================== Size forecast parsing ===================*/

    public static final String SIZE_DESCRIPTION =
        "In order to decide before launching a batch execution whether it will overflow the"
        + " size quota, we need to have an even imprecise forecast of how much space the execution"
        + " will occupy. This is a string whose pattern is an optional '*', a number, a letter."
        + " The letter is the unit: 'b' for byte, 'k' for kilobytes, 'm' for megabytes, 'g' for gigabytes."
        + " If the value starts with '*', it will be a multiplier per megabyte of input data."
        + " For instance: '1g' means the result expected size is 1 gigabyte, no matter the input size."
        + " Whereas '*2.5' means that for every megabyte in input, there will be 2.5 megabytes of"
        + " data in the output.";

    private static final Pattern SIZE_FORECAST_REGEXP = Pattern.compile(
        "^(?:(?<mult>\\*(?<mnum>\\d+(?:\\.\\d*)?))|(?<abs>(?<anum>\\d+(?:\\.\\d*)?)(?<unit>[bkmg])))$");

    @Override
    public Try<IResultSizeForecast> parseResultSizeForecast(String str) {
        return mapFailure(str, Try.of(() -> {
            // Remove whitespace and convert to lowercase
            Matcher matcher = SIZE_FORECAST_REGEXP.matcher(cleanup(str));
            if (matcher.matches()) {
                String multGroup = matcher.group("mult");
                if (multGroup != null) {
                    double num = Double.parseDouble(matcher.group("mnum"));
                    return new MultiplierResultSizeForecast(num);
                } else {
                    double num = Double.parseDouble(matcher.group("anum"));
                    long unit = bytesPerUnit.get(matcher.group("unit")).getOrElseThrow(() -> unparsable(str));
                    return new AbsoluteResultSizeForecast((long) (num * unit));
                }
            } else {
                throw unparsable(str);
            }
        }));
    }

    /*=================== Duration forecast parsing ===================*/

    public static final String DURATION_DESCRIPTION =
        "In order to detect executions which have silently stopped working, we need an even"
        + " imprecise estimation of the duration the execution will take. The processing module will"
        + " take this duration, and multiply by a constant configurable value in order to define"
        + " a timeout. Examples: '10s' for 10 seconds, '5min' for 5 minutes, '4h' for 4 hours,"
        + " '2d' for 2 days ; '10s/m' for 10 seconds per megabyte of input data ; '4h/g' for 4 hours"
        + " per gigabyte of input data.";

    private static final Pattern DURATION_FORECAST_REGEXP = Pattern.compile(
        "^(?<num>\\d+(?:\\.\\d*)?)(?<tunit>s|min|h|d)(?<persize>/[bkmg])?$");

    @Override
    public Try<IRunningDurationForecast> parseRunningDurationForecast(String str) {
        return mapFailure(str, Try.of(() -> {
            // Remove whitespace and convert to lowercase
            Matcher matcher = DURATION_FORECAST_REGEXP.matcher(cleanup(str));
            if (matcher.matches()) {
                double num = Double.parseDouble(matcher.group("num"));
                long tunit = millisPerUnit.get(matcher.group("tunit")).getOrElseThrow(() -> unparsable(str));
                String persizeGroup = Option.of(matcher.group("persize")).getOrElse("");
                if (!persizeGroup.isEmpty()) {
                    String persizeUnit = persizeGroup.replaceFirst("^/", "");
                    Long bytes = bytesPerUnit.get(persizeUnit).getOrElseThrow(() -> unparsable(str));
                    return new MultiplierRunningDurationForecast(num * tunit / bytes);
                } else {
                    return new AbsoluteRunningDurationForecast((long) (num * tunit));
                }
            } else {
                throw unparsable(str);
            }
        }));
    }

    private String cleanup(String str) {
        return str.replaceAll("\\s+", "").toLowerCase();
    }

    static class ForecastParserException extends Exception {

        public ForecastParserException(String s) {
            super(s);
        }

        public ForecastParserException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }

    private ForecastParserException unparsable(String str) {
        return new ForecastParserException("Unparsable forecast '" + str + "'");
    }

    private ForecastParserException unparsable(String str, Throwable parent) {
        return new ForecastParserException("Unparsable forecast '" + str + "'", parent);
    }

    private <T> Try<T> mapFailure(String str, Try<T> t) {
        return t.mapFailure(Case($(not(instanceOf(ForecastParserException.class))), e -> unparsable(str, e)));
    }

}
