package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractMarshallingTest<T> {

    public abstract Class<T> testedType();

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMarshallingTest.class);

    private final Gson gson = GsonProcessingTestUtils.gson();

    @Test
    public void test_toJson_fromJson() {
        Class<T> testedType = testedType();
        for (int i = 0; i < 100 ; i++) {
            T expected = randomInstance(testedType);
            String expectedJson = gson.toJson(expected);
            T actual = gson.fromJson(expectedJson, testedType);
            String actualJson = gson.toJson(actual);
            boolean equal = actualJson.equals(expectedJson);
            if (!equal) {
                LOGGER.error("Different values for {}: \n    FROM: {}\n    TO  : {}", testedType, expectedJson, actualJson);
                LOGGER.error("Different values for {}: \n    FROM: {}\n    TO  : {}", testedType, expected, actual);
            }
            assertThat(actualJson).isEqualTo(expectedJson);
            assertThat(actual).isEqualTo(expected);
        }
    }

}
