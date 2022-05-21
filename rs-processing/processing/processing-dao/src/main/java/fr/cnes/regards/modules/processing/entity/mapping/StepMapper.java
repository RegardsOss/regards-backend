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
package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.entity.StepEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * This class define a mapper between domain and database entities for Steps
 *
 * @author gandrieu
 */

@Component
public class StepMapper implements DomainEntityMapper.Step {

    public StepEntity toEntity(PStep step) {
        return new StepEntity(step.getStatus(), step.getTime().toEpochSecond() * 1000L, step.getMessage());
    }

    public PStep toDomain(StepEntity entity) {
        return PStep.from(entity.getStatus(),
                          OffsetDateTime.ofInstant(Instant.ofEpochMilli(entity.getEpochTs()), ZoneId.of("UTC")),
                          entity.getMessage());
    }
}
