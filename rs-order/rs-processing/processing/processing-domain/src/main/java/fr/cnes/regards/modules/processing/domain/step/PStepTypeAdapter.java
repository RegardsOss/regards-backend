package fr.cnes.regards.modules.processing.domain.step;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.utils.TypedGsonTypeAdapter;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;

@AutoService(TypedGsonTypeAdapter.class)
public class PStepTypeAdapter implements TypedGsonTypeAdapter<PStep> {

    @Override public Class<PStep> type() {
        return PStep.class;
    }

    @Override public JsonDeserializer<PStep> deserializer() {
        return (json, typeOfT, context) -> {
            ExecutionStatus status = context.deserialize(json.getAsJsonObject().get("status"), ExecutionStatus.class);
            String message = context.deserialize(json.getAsJsonObject().get("message"), String.class);
            OffsetDateTime time = context.deserialize(json.getAsJsonObject().get("time"), OffsetDateTime.class);
            return PStep.from(status, time, message);
        };
    }

    @Override public JsonSerializer<PStep> serializer() {
        return (src, typeOfSrc, context) ->
            src instanceof PStepFinal
                ? context.serialize(src, PStepFinal.class)
                : context.serialize(src, PStepIntermediary.class);
    }
}
