package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonConverterTest {

    public static final String SAMPLE_JSON_PATH = "json/convert-gson-to-xcontentbuilder.json";

    @Test
    public void test_toXContentBuilder_fine() throws IOException {
        // GIVEN
        String json = IOUtils.toString(getSystemResourceAsStream(SAMPLE_JSON_PATH), StandardCharsets.UTF_8).trim();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        // WHEN
        XContentBuilder builder = new JsonConverter().toXContentBuilder(jsonObject);

        // THEN
        String result = Strings.toString(builder.prettyPrint()).trim();
        String bullshitsRegexp = "\\s*";
        assertThat(json.replaceAll(bullshitsRegexp, "")).isEqualTo(result.replaceAll(bullshitsRegexp, ""));
    }

}