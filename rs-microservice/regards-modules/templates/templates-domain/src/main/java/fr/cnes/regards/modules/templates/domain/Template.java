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
package fr.cnes.regards.modules.templates.domain;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

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
     * A human readable code identifying the template
     */
    @NotBlank
    @Column(name = "code", nullable = false, length = 100)
    private final String code;

    /**
     * The template as a string for db persistence
     */
    @NotBlank
    @Column(name = "content")
    @Type(type = "text")
    private String content;

    /**
     * For a specific template, this attribute is intendend to store the skeleton of values to be injected in the
     * template
     */
    @NotNull
    @ElementCollection
    @MapKeyColumn(name = "name", length = 48)
    @Column(name = "value", length = 128)
    @CollectionTable(name = "t_template_data", joinColumns = @JoinColumn(name = "template_id"),
            foreignKey = @ForeignKey(name = "fk_template_data_template_id"))
    private Map<String, String> dataStructure;

    /**
     * A subject if the template should be written to something with a subject, title...
     */
    @NotBlank
    @Column(name = "subject", length = 100)
    private final String subject;

    /**
     * A description for the template
     */
    @Column(name = "description", length = 100)
    private String description;

    /**
     * Create a new {@link Template} with default values.
     */
    public Template() {
        super();
        code = "DEFAULT";
        content = "Hello $name.";
        dataStructure = new HashMap<>();
        dataStructure.put("name", "Defaultname");
        subject = "Default subject";
    }

    /**
     * @param pCode the code
     * @param pContent the content
     * @param pData the data
     * @param pSubject the subject if the template should be written to something with a subject or title (like an email)
     */
    public Template(final String pCode, final String pContent, final Map<String, String> pData, final String pSubject) {
        super();
        code = pCode;
        content = pContent;
        dataStructure = pData;
        subject = pSubject;
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
     * @return the dataStructure
     */
    public Map<String, String> getDataStructure() {
        return dataStructure;
    }

    /**
     * @param pDataStructure the data structure to set
     */
    public void setDataStructure(final Map<String, String> pDataStructure) {
        dataStructure = pDataStructure;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param pDescription the description to set
     */
    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
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

        return code.equals(template.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
