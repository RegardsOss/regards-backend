/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProcessExecDescription extends ProcessBatchDescription {

    private final List<String> inputDataFiles;

    public ProcessExecDescription(
            UUID processBusinessId,
            Map<String, String> parameters,
            UUID batchId,
            List<String> inputDataFiles
    ) {
        super(processBusinessId, parameters, batchId);
        this.inputDataFiles = inputDataFiles;
    }

    public ProcessExecDescription(ProcessBatchDescription desc, List<String> inputDataFiles) {
        super(desc, desc.getBatchId());
        this.inputDataFiles = inputDataFiles;
    }

    public List<String> getInputDataFiles() {
        return inputDataFiles;
    }
}
