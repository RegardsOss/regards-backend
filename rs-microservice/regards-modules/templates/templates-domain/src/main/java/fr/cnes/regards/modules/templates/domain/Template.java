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
package fr.cnes.regards.modules.templates.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Domain class representing a template.<br>
 * A template defines a content using named keys with a $ ($toto for example)
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 */
@Entity
@Table(name = "t_template", uniqueConstraints = @UniqueConstraint(name = "uk_template_code", columnNames = { "code" }))
@SequenceGenerator(name = "templateSequence", initialValue = 1, sequenceName = "seq_template")
public class Template implements IIdentifiable<Long> {

    /**
     * The id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "templateSequence")
    private Long id;

    /**
     * A human readable name identifying the template.
     * Attribute and column does not have the same name for compatibility issue between versions
     */
    @NotBlank
    @Column(name = "code", nullable = false, length = 100, updatable = false)
    private String name;

    /**
     * The template as a string for db persistence
     */
    @NotBlank
    @Column(name = "content")
    @Type(type = "text")
    private String content;

    /**
     * Create a new {@link Template} with default values.
     */
    public Template() {
    }

    /**
     * @param name the name
     * @param content the content
     */
    public Template(String name, String content) {
        this.name = name;
        this.content = content;
    }

    /**
     * @return the id
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * @param pId the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param pContent the content to set
     */
    public void setContent(final String pContent) {
        content = pContent;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Template template = (Template) o;

        return name.equals(template.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
