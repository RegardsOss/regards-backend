package fr.cnes.regards.modules.processing.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.gson.VavrGson;

public class GsonProcessingUtils {

    public static Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        VavrGson.registerAll(builder);
        return builder.create();
    }

}
