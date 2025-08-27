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

package fr.cnes.regards.modules.indexer.domain;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * Entity representing the indexing state of the REGARDS catalog.
 *
 * @author mnguyen0
 */
@Entity
@Table(name = "t_es_index_alias")
public class EsIndexAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "esIndexAliasSequence")
    @SequenceGenerator(name = "esIndexAliasSequence", sequenceName = "seq_es_index_alias", allocationSize = 50)
    private Long id;

    /**
     * Name of the alias. Must be unique.
     */
    @Column(nullable = false, unique = true)
    private String alias;

    /**
     * Name of the currently active index.
     */
    @Column(name = "current_index", nullable = false)
    private String current;

    /**
     * Name of the building index, null if there is no one currently building
     */
    @Column(name = "building_index")
    private String building;

    public EsIndexAlias() {
    }

    public EsIndexAlias(String alias, String current) {
        this.alias = alias;
        this.current = current;
    }

    public String getAlias() {
        return alias;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EsIndexAlias that = (EsIndexAlias) o;
        return Objects.equals(alias, that.alias);

    }

    @Override
    public int hashCode() {
        return Objects.hash(alias);
    }

    @Override
    public String toString() {
        return "EsIndexAlias{"
               + "id="
               + id
               + ", alias='"
               + alias
               + '\''
               + ", current='"
               + current
               + '\''
               + ", building='"
               + building
               + '\''
               + '}';
    }
}
