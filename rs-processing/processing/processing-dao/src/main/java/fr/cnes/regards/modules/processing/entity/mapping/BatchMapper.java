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

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.FileStatsByDataset;
import fr.cnes.regards.modules.processing.entity.ParamValues;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.springframework.stereotype.Component;

/**
 * This class define a mapper between domain and database entities for Batch
 *
 * @author gandrieu
 */
@Component
public class BatchMapper implements DomainEntityMapper.Batch {

    @Override
    public BatchEntity toEntity(PBatch batch) {
        return new BatchEntity(
                batch.getId(),
                batch.getProcessBusinessId(),
                batch.getCorrelationId(),
                batch.getTenant(),
                batch.getUser(),
                batch.getUserRole(),
                new ParamValues(batch.getUserSuppliedParameters().toJavaList()),
                new FileStatsByDataset(batch.getFilesetsByDataset().toJavaMap()), batch.isPersisted());
    }

    @Override
    public PBatch toDomain(BatchEntity entity) {
        return new PBatch(
                entity.getCorrelationId(),
                entity.getId(),
                entity.getProcessBusinessId(),
                entity.getTenant(),
                entity.getUserEmail(),
                entity.getUserRole(),
                List.ofAll(entity.getParameters().getValues()),
                HashMap.ofAll(entity.getFilesets().getMap()),
                entity.isPersisted()
        );
    }

}
