package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.utils.gson.ProcessingGsonUtils;

public class GsonProcessingTestUtils {

    public static Gson gson() {
        return ProcessingGsonUtils.gsonPretty();
    }

}
