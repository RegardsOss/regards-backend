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
package fr.cnes.regards.modules.feature.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;

/**
 *  Serive to create {@link DataObjectFeature} from {@link FeatureEntity}
 *  @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class DataObjectFeatureService implements IDataObjectFeatureService {

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Override
    public Page<DataObjectFeature> findAll(String model, Pageable pageable, OffsetDateTime date) {
        Page<FeatureEntity> entities = this.featureRepo.findByModelAndLastUpdateAfter(model, date, pageable);
        List<DataObjectFeature> elements = entities.stream().map(entity -> initDataObjectFeature(entity))
                .collect(Collectors.toList());
        return new PageImpl<DataObjectFeature>(elements, pageable, entities.getTotalElements());
    }

    private DataObjectFeature initDataObjectFeature(FeatureEntity entity) {
        DataObjectFeature dof = new DataObjectFeature(entity.getUrn(), entity.getProviderId(), "NO LABEL");
        dof.setProperties(entity.getFeature().getProperties());
        dof.setSession(entity.getSession());
        dof.setSessionOwner(entity.getSessionOwner());
        dof.setModel(entity.getModel());
        return dof;
    }

}
