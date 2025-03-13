package fr.cnes.regards.modules.crawler.service.handler;

/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequest;
import fr.cnes.regards.modules.crawler.service.service.DatasetAttributeModelService;
import fr.cnes.regards.modules.dam.domain.entities.event.DatasetEvent;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.event.ComputedAttributeModelEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handler for Spring event {@link ComputedAttributeModelEvent}
 *
 * @author Jean-Christophe HUNOUT
 **/
@Component
public class ComputeAttributeModelSpringEventHandler {

    @Autowired
    private DatasetAttributeModelService datasetAttributeModelService;

    @EventListener
    @MultitenantTransactional
    public void onComputedAttributeModelEvent(ComputedAttributeModelEvent event) {
        ModelAttrAssoc modelAttrAssoc = event.getSource();
        datasetAttributeModelService.computeAttributeModel(modelAttrAssoc);
    }
}
