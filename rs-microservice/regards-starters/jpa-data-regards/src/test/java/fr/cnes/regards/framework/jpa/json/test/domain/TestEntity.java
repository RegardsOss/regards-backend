/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.json.test.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_test_entity")
@SequenceGenerator(name = "testEntitySequence", initialValue = 1, sequenceName = "seq_test_entity")
public class TestEntity {

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "testEntitySequence")
    private Long id;

    /**
     * jsonb field
     */
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonbEntity jsonbEntity;

    public TestEntity() {
        super();
    }

    public TestEntity(JsonbEntity pJsonbEntity) {
        this();
        jsonbEntity = pJsonbEntity;
    }

    public TestEntity(Long pId, JsonbEntity pJsonbEntity) {
        this(pJsonbEntity);
        id = pId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public JsonbEntity getJsonbEntity() {
        return jsonbEntity;
    }

    public void setJsonbEntity(JsonbEntity pJsonbEntity) {
        jsonbEntity = pJsonbEntity;
    }

    @Override
    public String toString() {
        return "TestEntity { id = " + id + ", jsonbEntity = " + jsonbEntity + "}";
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof TestEntity) && ((TestEntity) pOther).id.equals(id)
                && ((TestEntity) pOther).jsonbEntity.equals(jsonbEntity);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
