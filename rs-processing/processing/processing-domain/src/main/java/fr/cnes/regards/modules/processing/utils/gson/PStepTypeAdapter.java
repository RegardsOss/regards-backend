/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.auto.service.AutoService;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;

import java.time.OffsetDateTime;


/**
 * This class is a Gson type adapter for {@link PStep}.
 *
 * @author gandrieu
 */
@AutoService(TypedGsonTypeAdapter.class)
public class PStepTypeAdapter implements TypedGsonTypeAdapter<PStep> {

    @Override
    public Class<PStep> type() {
        return PStep.class;
    }

    @Override
    public JsonDeserializer<PStep> deserializer() {
        return (json, typeOfT, context) -> {
            ExecutionStatus status = context.deserialize(json.getAsJsonObject().get("status"), ExecutionStatus.class);
            String message = context.deserialize(json.getAsJsonObject().get("message"), String.class);
            OffsetDateTime time = context.deserialize(json.getAsJsonObject().get("time"), OffsetDateTime.class);
            return PStep.from(status, time, message);
        };
    }

    @Override
    public JsonSerializer<PStep> serializer() {
        return (src, typeOfSrc, context) -> src instanceof PStepFinal ? context.serialize(src, PStepFinal.class)
                : context.serialize(src, PStepIntermediary.class);
    }
}
