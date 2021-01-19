/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *
 * @author gandrieu
 */
package fr.cnes.regards.modules.order.service.job;

import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;

public class BasketDatasetSelectionDescriptor {

    private final Long dsSelId;
    private final String datasetIpId;
    private final ProcessDatasetDescription processDatasetDescription;

    public BasketDatasetSelectionDescriptor(Long dsSelId, String datasetIpId, ProcessDatasetDescription processDatasetDescription) {
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

    public ProcessDatasetDescription getProcessDatasetDescription() {
        return processDatasetDescription;
    }

}
