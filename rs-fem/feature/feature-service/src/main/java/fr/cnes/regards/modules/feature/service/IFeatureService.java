/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.RecipientsSearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

/**
 * Factory for {@link FeatureEntityDto} to init according {@link FeatureEntity}
 *
 * @author Kevin Marchois
 */
public interface IFeatureService {

    /**
     * Get a {@link Page} of {@link FeatureEntityDto} dto
     * The {@link Page} will be initialized from a list of {@link FeatureEntityDto}
     *
     * @param filters {@link SearchFeatureSimpleEntityParameters} search filters
     * @return {@link Page} of {@link FeatureEntityDto}
     */
    Page<FeatureEntityDto> findAll(SearchFeatureSimpleEntityParameters filters, Pageable pageable);

    /**
     * Get a {@link FeatureEntityDto} by its urn
     *
     * @param urn {@link FeatureUniformResourceName}
     * @return {@link FeatureEntityDto}
     */
    FeatureEntityDto findOne(FeatureUniformResourceName urn);

    /**
     * Creates a job to creates new notification requests for all features matching selection parameters
     *
     * @param selection {@link SearchFeatureSimpleEntityParameters}
     */
    JobInfo scheduleNotificationsJob(RecipientsSearchFeatureSimpleEntityParameters selection);

    /**
     * Creates a job to creates new deletion requests for all features matching selection parameters
     *
     * @param selection {@link SearchFeatureSimpleEntityParameters}
     */
    JobInfo scheduleDeletionJob(SearchFeatureSimpleEntityParameters selection);
}
