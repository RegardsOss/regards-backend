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
package fr.cnes.regards.modules.crawler.domain;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Deletion request for an indexed entity.
 *
 * @author Thibaud Michaudel
 **/
@Entity
@Table(name = "t_entity_deletion_request")
public class EntityDeletionRequest {

    @Id
    @SequenceGenerator(name = "deleteRequestSequence", initialValue = 1, sequenceName = "seq_deletion_request")
    @GeneratedValue(generator = "deleteRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    public EntityDeletionRequest(String entityId) {
        this.entityId = entityId;
    }

    public EntityDeletionRequest() {
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityDeletionRequest that = (EntityDeletionRequest) o;
        return Objects.equals(id, that.id) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entityId);
    }

    @Override
    public String toString() {
        return "EntityDeletionRequest{" + "id=" + id + ", entityId='" + entityId + '\'' + '}';
    }
}
