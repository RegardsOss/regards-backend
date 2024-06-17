/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * TODO: BasketDatasetSelectionDescriptor description
 */
package fr.cnes.regards.modules.order.service.job;

import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.dto.dto.ProcessDatasetDescriptionDto;

public class BasketDatasetSelectionDescriptor {

    private final Long dsSelId;

    private final String datasetIpId;

    private final ProcessDatasetDescriptionDto processDatasetDescription;

    public BasketDatasetSelectionDescriptor(Long dsSelId,
                                            String datasetIpId,
                                            ProcessDatasetDescriptionDto processDatasetDescription) {
        this.dsSelId = dsSelId;
        this.datasetIpId = datasetIpId;
        this.processDatasetDescription = processDatasetDescription;
    }

    public BasketDatasetSelectionDescriptor(BasketDatasetSelection dsSel) {
        this(dsSel.getId(), dsSel.getDatasetIpid(), dsSel.getProcessDatasetDescription());
    }

    public Long getDsSelId() {
        return dsSelId;
    }

    public String getDatasetIpId() {
        return datasetIpId;
    }

    public ProcessDatasetDescriptionDto getProcessDatasetDescription() {
        return processDatasetDescription;
    }

}
