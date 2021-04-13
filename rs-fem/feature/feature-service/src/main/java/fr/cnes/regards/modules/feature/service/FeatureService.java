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
package fr.cnes.regards.modules.feature.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.dao.FeatureEntitySpecification;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;

/**
 *  Serive to create {@link DataObjectFeature} from {@link FeatureEntity}
 *  @author Kevin Marchois
 *  @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class FeatureService implements IDataObjectFeatureService {

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Override
    public Page<FeatureEntityDto> findAll(FeaturesSelectionDTO selection, Pageable page) {
        Page<FeatureEntity> entities = featureRepo
                .findAll(FeatureEntitySpecification.searchAllByFilters(selection, page), page);
        List<FeatureEntityDto> elements = entities.stream().map(entity -> initDataObjectFeature(entity))
                .collect(Collectors.toList());
        return new PageImpl<FeatureEntityDto>(elements, page, entities.getTotalElements());
    }

    private FeatureEntityDto initDataObjectFeature(FeatureEntity entity) {
        FeatureEntityDto dto = new FeatureEntityDto();
        dto.setSession(entity.getSession());
        dto.setSessionOwner(entity.getSessionOwner());
        dto.setFeature(entity.getFeature());
        dto.setProviderId(entity.getProviderId());
        dto.setVersion(entity.getVersion());
        dto.setLastUpdate(entity.getLastUpdate());
        return dto;
    }

}
