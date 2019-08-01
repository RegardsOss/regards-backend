
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Class User
 *
 * JPA Company Entity. For projects multitenancy databases.
 * @author CS
 */
@Entity
@Table(name = "t_user")
@SequenceGenerator(name = "userSequence", initialValue = 1, sequenceName = "seq_user")
public class User {

    /**
     * User identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSequence")
    @Column(name = "id")
    private Long id;

    /**
     * User first name
     */
    @Column(name = "firstname")
    private String firstName;

    /**
     * User last name
     */
    @Column(name = "lastname")
    private String lastName;

    /**
     * User's company
     */
    @ManyToOne
    @JoinColumn(name = "company_id", foreignKey = @javax.persistence.ForeignKey(name = "fk_user_company"))
    private Company company;

    /**
     * Constructor
     */
    public User() {

    }

    /**
     * Constructor
     * @param pFirstName User first name
     * @param pLastName User last name
     */
    public User(String pFirstName, String pLastName) {
        super();
        this.firstName = pFirstName;
        this.lastName = pLastName;
    }

    /**
     * Constructor
     * @param pFirstName User first name
     * @param pLastName User last name
     * @param pCompany User's Company
     */
    public User(String pFirstName, String pLastName, Company pCompany) {
        super();
        this.firstName = pFirstName;
        this.lastName = pLastName;
        this.company = pCompany;
    }

    /**
     * Getter
     * @return User identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter
     * @param pId User identifier
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * Getter
     * @return User firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Getter
     * @return User lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Getter
     * @return User's company
     */
    public Company getCompany() {
        return company;
    }

    /**
     * Setter
     * @param pFirstName User firstName
     */
    public void setFirstName(String pFirstName) {
        firstName = pFirstName;
    }

    /**
     * Setter
     * @param pLastName User lastName
     */
    public void setLastName(String pLastName) {
        lastName = pLastName;
    }

    /**
     * Setter
     * @param pCompany User's company
     */
    public void setCompany(Company pCompany) {
        company = pCompany;
    }

}
