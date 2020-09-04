package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.utils.ProcessingGsonUtils;

public class GsonProcessingTestUtils {

    public static Gson gson() {
        return ProcessingGsonUtils.gsonPretty();
    }

}
