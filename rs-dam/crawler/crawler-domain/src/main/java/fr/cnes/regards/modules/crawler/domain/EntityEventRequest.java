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

import fr.cnes.regards.framework.security.role.DefaultRole;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * Entity for a request event (create, update...) on an entity (identified by its urn) that need to be
 * updated in the elasticsearch repository.
 *
 * @author Thibaud Michaudel
 **/
@Entity
@Table(name = "t_entity_event_request")
public class EntityEventRequest {

    @Id
    @SequenceGenerator(name = "entityEventRequestSequence", initialValue = 1, sequenceName = "entity_event_request")
    @GeneratedValue(generator = "entityEventRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, name = "urn")
    private String urn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private EntityEventRequestStatus status;

    @Column(name = "userToNotify")
    private String userToNotify;

    @Column(name = "roleToNotify")
    private String roleToNotify;

    public EntityEventRequest(String urn) {
        this(urn, null, DefaultRole.INSTANCE_ADMIN.name());
    }

    public EntityEventRequest(String urn, String userToNotify, String roleToNotify) {
        this.urn = urn;
        this.status = EntityEventRequestStatus.TO_DO;
        this.userToNotify = userToNotify;
        this.roleToNotify = roleToNotify;
    }

    public EntityEventRequest() {
        // empty constructor for Spring
    }

    public Long getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public String getUserToNotify() {
        return userToNotify;
    }

    public String getRoleToNotify() {
        return roleToNotify;
    }

    public EntityEventRequestStatus getStatus() {
        return status;
    }

    public void setStatus(EntityEventRequestStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityEventRequest request = (EntityEventRequest) o;
        return Objects.equals(id, request.id) && Objects.equals(urn, request.urn) && status == request.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, urn, userToNotify, roleToNotify, status);
    }

    @Override
    public String toString() {
        return "EntityEventRequest{"
               + "id="
               + id
               + ", urn='"
               + urn
               + ", userToNotify='"
               + userToNotify
               + ", roleToNotify='"
               + roleToNotify
               + '\''
               + ", status="
               + status
               + '}';
    }
}
