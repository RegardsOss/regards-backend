package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonMergerTest {

    public static final String MERGE1_JSON_PATH = "json/merge-one.json";

    public static final String MERGE2_JSON_PATH = "json/merge-two.json";

    public static final String MERGED_JSON_PATH = "json/merged.json";

    @Test
    public void test_merge() throws IOException {
        // GIVEN
        String merge1 = IOUtils.toString(getSystemResourceAsStream(MERGE1_JSON_PATH), "UTF-8").trim();
        String merge2 = IOUtils.toString(getSystemResourceAsStream(MERGE2_JSON_PATH), "UTF-8").trim();
        String expected = IOUtils.toString(getSystemResourceAsStream(MERGED_JSON_PATH), "UTF-8").trim();

        JsonObject jsonMerge1 = new JsonParser().parse(merge1).getAsJsonObject();
        JsonObject jsonMerge2 = new JsonParser().parse(merge2).getAsJsonObject();
        JsonObject jsonExpected = new JsonParser().parse(expected).getAsJsonObject();

        // WHEN
        JsonObject jsonMerged = new JsonMerger().merge(jsonMerge1, jsonMerge2);

        // THEN
        assertThat(jsonMerged).isEqualTo(jsonExpected);

    }
}