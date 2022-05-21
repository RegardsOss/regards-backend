/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.pojo;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

import javax.persistence.*;

/**
 * Class Project
 * <p>
 * JPA Project Entity. For instance database.
 *
 * @author CS
 */
@Entity
@Table(name = "t_project")
@InstanceEntity
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "seq_project")
public class TestProject {

    /**
     * Project identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Project name
     */
    @Column(name = "name")
    private String name;

    /**
     * Getter
     *
     * @return Project identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter
     *
     * @param pId Project identifier
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * Getter
     *
     * @return Project name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter
     *
     * @param pName Project name
     */
    public void setName(String pName) {
        name = pName;
    }

}
