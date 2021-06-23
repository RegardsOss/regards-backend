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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serive to create {@link DataObjectFeature} from {@link FeatureEntity}
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class DataObjectFeatureService implements IDataObjectFeatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataObjectFeatureService.class);

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Override
    public Page<FeatureEntityDto> findAll(String model, Pageable pageable, OffsetDateTime date) {
        Page<FeatureEntity> entities;
        if (date == null) {
            entities = featureRepo.findByModel(model, pageable);
        } else {
            entities = featureRepo.findByModelAndLastUpdateAfter(model, date, pageable);
        }
        LOGGER.info(
                "Page {} of max {} entities with model {} and last update after {} requested : {}/{} found (Total page(s):{})",
                pageable.getPageNumber(), pageable.getPageSize(), model, date, entities.getNumberOfElements(),
                entities.getTotalElements(), entities.getTotalPages());

        List<FeatureEntityDto> elements = entities.stream().map(entity -> initDataObjectFeature(entity))
                .collect(Collectors.toList());
        return new PageImpl<FeatureEntityDto>(elements, pageable, entities.getTotalElements());
    }

    private FeatureEntityDto initDataObjectFeature(FeatureEntity entity) {
        FeatureEntityDto dto = new FeatureEntityDto();
        dto.setSession(entity.getSession());
        dto.setSource(entity.getSessionOwner());
        dto.setFeature(entity.getFeature());
        return dto;
    }

}
