package fr.cnes.regards.modules.processing.forecast;

import fr.cnes.regards.modules.processing.forecast.ForecastParser;
import org.junit.Test;

import java.time.Duration;

import static fr.cnes.regards.modules.processing.forecast.ForecastParser.*;
import static org.assertj.vavr.api.VavrAssertions.assertThat;

public class ForecastParserTest {

    ForecastParser parser = INSTANCE;

    @Test public void parseResultSizeForecast() {
        assertThat(parser.parseResultSizeForecast("2m").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_BYTE)))
                .contains(2L * BYTES_IN_ONE_MEGABYTE);
        assertThat(parser.parseResultSizeForecast("2m  ").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains(2L * BYTES_IN_ONE_MEGABYTE);
        assertThat(parser.parseResultSizeForecast(" 10.5k ").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains((long) (10.5 * BYTES_IN_ONE_KILOBYTE));
        assertThat(parser.parseResultSizeForecast(" 012.3540 b ").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains(12L);

        assertThat(parser.parseResultSizeForecast("*2").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_BYTE)))
                .contains(20L * BYTES_IN_ONE_BYTE);
        assertThat(parser.parseResultSizeForecast("*0.5  ").map(r -> r.expectedResultSizeInBytes(20L * BYTES_IN_ONE_GIGABYTE)))
                .contains(10L * BYTES_IN_ONE_GIGABYTE);
        assertThat(parser.parseResultSizeForecast(" * 10 ").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains((long) (100 * BYTES_IN_ONE_GIGABYTE));
        assertThat(parser.parseResultSizeForecast("  * 010 ").map(r -> r.expectedResultSizeInBytes(10L * BYTES_IN_ONE_MEGABYTE)))
                .contains(100L * BYTES_IN_ONE_MEGABYTE);
    }

    @Test public void parseRunningDurationForecast() {
        assertThat(parser.parseRunningDurationForecast("2s").map(r -> r.expectedRunningDurationInBytes(10L * BYTES_IN_ONE_BYTE)))
                .contains(Duration.ofMillis(2L * MILLIS_IN_ONE_SECOND));

        assertThat(parser.parseRunningDurationForecast("2min  ").map(r -> r.expectedRunningDurationInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains(Duration.ofMillis(2L * MILLIS_IN_ONE_MINUTE));

        assertThat(parser.parseRunningDurationForecast(" 10.5h ").map(r -> r.expectedRunningDurationInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains(Duration.ofMillis((long) (10.5 * MILLIS_IN_ONE_HOUR)));

        assertThat(parser.parseRunningDurationForecast(" 012.3540 d ").map(r -> r.expectedRunningDurationInBytes(10L * BYTES_IN_ONE_GIGABYTE)))
                .contains(Duration.ofMillis((long) (12.3540 * MILLIS_IN_ONE_DAY)));

        assertThat(parser.parseRunningDurationForecast("2s/k").map(r -> r.expectedRunningDurationInBytes(10L * BYTES_IN_ONE_KILOBYTE)))
                .contains(Duration.ofMillis(20L * MILLIS_IN_ONE_SECOND));


    }
}