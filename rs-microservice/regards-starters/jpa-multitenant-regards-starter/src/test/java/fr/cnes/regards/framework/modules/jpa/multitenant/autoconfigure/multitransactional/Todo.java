
/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.multitransactional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Basic pojo to test transaction synchronization
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_todo")
@SequenceGenerator(name = "todoSequence", initialValue = 1, sequenceName = "seq_todo")
public class Todo {

    /**
     * User identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todoSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Todo label
     */
    @Column(unique = true)
    private String label;

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String pLabel) {
        label = pLabel;
    }

    @Override
    public boolean equals(Object pObj) {
        if (pObj instanceof Todo) {
            Todo todo = (Todo) pObj;
            return label.equals(todo.getLabel());
        }
        return false;
    }
}
