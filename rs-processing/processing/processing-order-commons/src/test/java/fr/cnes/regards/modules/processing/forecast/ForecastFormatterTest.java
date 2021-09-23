package fr.cnes.regards.modules.processing.forecast;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ForecastFormatterTest {

    @Test
    public void test_format_absolute_size() {
        ForecastParser parser = ForecastParser.INSTANCE;
        AbsoluteResultSizeForecast absolute = new AbsoluteResultSizeForecast(12L);
        assertThat(parser.parseResultSizeForecast(absolute.format())).contains(absolute);
    }

    @Test
    public void test_format_multiplier_size() {
        ForecastParser parser = ForecastParser.INSTANCE;
        MultiplierResultSizeForecast multiplier = new MultiplierResultSizeForecast(2.5D);
        assertThat(parser.parseResultSizeForecast(multiplier.format())).contains(multiplier);
    }

}