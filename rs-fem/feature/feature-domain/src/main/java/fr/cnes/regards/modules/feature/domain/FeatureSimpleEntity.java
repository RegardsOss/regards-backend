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
package fr.cnes.regards.modules.feature.domain;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "t_feature",
        indexes = {
                @Index(name = "idx_feature_last_update", columnList = "last_update"),
                @Index(name = "idx_feature_urn", columnList = "urn"),
                @Index(name = "idx_feature_session", columnList = "session_owner,session_name"),
                @Index(name = "idx_feature_provider_id", columnList = "provider_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_feature_urn", columnNames = {"urn"})}
)
public class FeatureSimpleEntity extends AbstractFeatureEntity {

}
