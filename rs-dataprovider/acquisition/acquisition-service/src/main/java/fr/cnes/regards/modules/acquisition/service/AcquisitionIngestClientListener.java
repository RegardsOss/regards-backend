/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.client.IIngestClientListener;
import fr.cnes.regards.modules.ingest.client.RequestInfo;

/**
 * Custom acquisition ingest client listener
 * @author Marc SORDI
 */
@Component
@MultitenantTransactional
public class AcquisitionIngestClientListener implements IIngestClientListener {

    @Autowired
    private IProductService productService;

    @Override
    public void onDenied(Collection<RequestInfo> infos) {
        productService.handleIngestedSIPFailed(infos);
    }

    @Override
    public void onGranted(Collection<RequestInfo> infos) {
        // Nothing to do
    }

    @Override
    public void onError(Collection<RequestInfo> infos) {
        productService.handleIngestedSIPFailed(infos);
    }

    @Override
    public void onSuccess(Collection<RequestInfo> infos) {
        productService.handleIngestedSIPSuccess(infos);
    }

}
