/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionIngestClientListener.class);

    @Autowired
    private IProductService productService;

    @Override
    public void onDenied(RequestInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGranted(RequestInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(RequestInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSuccess(RequestInfo info) {
        productService.handleSIPSuccess(info);
    }

}
