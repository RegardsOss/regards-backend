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
package fr.cnes.regards.modules.order.domain.process;

import java.util.Map;
import java.util.UUID;

public class ProcessBatchDescription extends ProcessDatasetDescription {

    private final UUID batchId;

    public ProcessBatchDescription(UUID processBusinessId, Map<String, String> parameters, UUID batchId) {
        super(processBusinessId, parameters);
        this.batchId = batchId;
    }

    public ProcessBatchDescription(ProcessDatasetDescription desc, UUID batchId) {
        super(desc.getProcessBusinessId(), desc.getParameters());
        this.batchId = batchId;
    }

    public UUID getBatchId() {
        return batchId;
    }
}
