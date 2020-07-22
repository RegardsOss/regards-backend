package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.gson.VavrGson;

public class GsonProcessingTestUtils {

    public static Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        VavrGson.registerAll(builder);
        return builder.create();
    }

}
