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
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import fr.cnes.regards.modules.processing.entity.StepEntity;

/**
 * This interface defines generic mapper signatures
 *
 * @author gandrieu
 */

public interface DomainEntityMapper<D, E> {

    E toEntity(D domain);

    D toDomain(E entity);

    interface Batch extends DomainEntityMapper<PBatch, BatchEntity> {

    }

    interface Execution extends DomainEntityMapper<PExecution, ExecutionEntity> {

    }

    interface OutputFile extends DomainEntityMapper<POutputFile, OutputFileEntity> {

    }

    interface Step extends DomainEntityMapper<PStep, StepEntity> {

    }

}

