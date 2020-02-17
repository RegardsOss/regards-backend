/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.database.request;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;

/**
 * @author sbinda
 *
 */
@Entity
@Table(name = "t_request_group", indexes = { @Index(name = "idx_t_request_group", columnList = "id") })
public class RequestGroup {

    @Id
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestType type;

    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @Column(name = "expiration_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    public static RequestGroup build(String groupId, FileRequestType type, OffsetDateTime expirationDate) {
        RequestGroup grp = new RequestGroup();
        grp.id = groupId;
        grp.type = type;
        grp.creationDate = OffsetDateTime.now();
        grp.expirationDate = expirationDate;
        return grp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileRequestType getType() {
        return type;
    }

    public void setType(FileRequestType type) {
        this.type = type;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isExpired() {
        if ((expirationDate != null) && OffsetDateTime.now().isAfter(expirationDate)) {
            return true;
        } else {
            return false;
        }
    }

}
