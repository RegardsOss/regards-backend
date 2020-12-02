/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Value @With
public class PBatch {

    String correlationId;

    UUID id;

    UUID processBusinessId;

    String processName;

    String tenant;

    String user;

    String userRole;

    Seq<ExecutionStringParameterValue> userSuppliedParameters;

    Map<String, FileSetStatistics> filesetsByDataset;

    transient boolean persisted;

    public PBatch asNew() { return this.withId(UUID.randomUUID()).withPersisted(false); }

}
