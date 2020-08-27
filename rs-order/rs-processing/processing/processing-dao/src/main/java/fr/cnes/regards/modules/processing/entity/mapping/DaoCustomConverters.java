package fr.cnes.regards.modules.processing.entity.mapping;

import com.google.gson.Gson;
import io.vavr.collection.List;

public class DaoCustomConverters {

    public static java.util.List<Object> getCustomConverters(Gson gson) {
        return List.<Object>of(
                new ParamValuesToJsonbConverter(gson),
                new JsonbToParamValuesConverter(gson),
                new FileStatsByDatasetToJsonbConverter(gson),
                new JsonbToFileStatsByDatasetConverter(gson),
                new FileParametersToJsonbConverter(gson),
                new JsonbToFileParametersConverter(gson),
                new StepsToJsonbConverter(gson),
                new JsonbToStepsConverter(gson)
        ).toJavaList();
    }

}
