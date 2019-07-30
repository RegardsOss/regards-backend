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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Class Company
 *
 * JPA Company Entity. For projects multitenancy databases.
 * @author CS
 */
@Entity
@Table(name = "t_company")
@SequenceGenerator(name = "companySequence", initialValue = 1, sequenceName = "seq_company")
public class Company {

    /**
     * Company identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companySequence")
    @Column(name = "id")
    private Long id;

    /**
     * Company name
     */
    @Column(name = "name")
    private String name;

    /**
     * Constructor
     */
    public Company() {

    }

    /**
     * Constructor
     * @param pName Company name
     */
    public Company(String pName) {
        super();
        name = pName;
    }

    /**
     * Getter
     * @return Company identifier
     * @since 1.0-SNPASHOT
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter
     * @param pId Company identifier
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * Getter
     * @return Company name
     * @since 1.0-SNPASHOT
     */
    public String getName() {
        return name;
    }

    /**
     * Setter
     * @param pName Company name
     */
    public void setName(String pName) {
        name = pName;
    }

}
