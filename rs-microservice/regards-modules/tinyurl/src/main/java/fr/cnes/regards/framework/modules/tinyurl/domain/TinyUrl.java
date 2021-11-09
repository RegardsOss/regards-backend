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
package fr.cnes.regards.framework.modules.tinyurl.domain;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Store context of a tiny URL
 *
 * @author Marc SORDI
 */

@Entity
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Table(name = "t_tinyurl", indexes = { @Index(name = "idx_tinyurl_uuid", columnList = "uuid") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_tinyurl_uuid", columnNames = { "uuid" }) })
public class TinyUrl {

    @Id
    @SequenceGenerator(name = "tinyurlSequence", initialValue = 1, sequenceName = "seq_tinyurl", allocationSize = 1000)
    @GeneratedValue(generator = "tinyurlSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 36, nullable = false, updatable = false)
    private String uuid;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(type = "jsonb")
    private JsonElement context;

    @Column(nullable = false, name = "class")
    private String classOfContext;

    @Column(nullable = false)
    private OffsetDateTime expirationDate;

    public Long getId() {
        return id;
    }

    public TinyUrl setId(Long id) {
        this.id = id;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public TinyUrl setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public JsonElement getContext() {
        return context;
    }

    public TinyUrl setContext(JsonElement context) {
        this.context = context;
        return this;
    }

    public String getClassOfContext() {
        return classOfContext;
    }

    public TinyUrl setClassOfContext(String classOfContext) {
        this.classOfContext = classOfContext;
        return this;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public TinyUrl setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
}
