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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.framework.jpa.utils.SliceRepositoryUtils;
import fr.cnes.regards.modules.feature.domain.FeatureSimpleRawEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Custom repository for Feature slices using specifications because the default jpa repository doesn't support the
 * combined usage of slices and specifications
 *
 * @author tguillou
 */
@Repository
public class FeatureSliceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Finds a slice of {@link FeatureSimpleRawEntity} entities based on the provided specification and slice size.
     */
    public Slice<FeatureSimpleRawEntity> findMore(Specification<FeatureSimpleRawEntity> spec, Pageable pageable) {
        return SliceRepositoryUtils.findSlice(entityManager, FeatureSimpleRawEntity.class, spec, pageable);
    }
}