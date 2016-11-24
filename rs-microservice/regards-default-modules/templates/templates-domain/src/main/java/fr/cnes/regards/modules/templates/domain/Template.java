/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Domain class representing a template.
 *
 * @author Xavier-Alexandre Brochard
 */
@Entity(name = "T_TEMPLATE")
@SequenceGenerator(name = "templateSequence", initialValue = 1, sequenceName = "SEQ_TEMPLATE")
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
    @Column(unique = true)
    private final String code;

    /**
     * The template as a string for db persistence
     */
    @NotBlank
    private String content;

    /**
     * For a specific template, this attribute is intendend to store the skeleton of values to be injected in the
     * template.
     */
    @NotNull
    @ElementCollection
    @CollectionTable(name = "TEMPLATE_DATA")
    private Map<String, String> data;

    /**
     * A description for the template
     */
    private String description;

    /**
     * Create a new {@link Template} with default values.
     */
    public Template() {
        super();
        code = "DEFAULT";
        content = "Hello $name.";
        data = new HashMap<>();
        data.put("name", "Defaultname");
    }

    /**
     * @param pCode
     * @param pContent
     * @param pData
     */
    public Template(final String pCode, final String pContent, final Map<String, String> pData) {
        super();
        code = pCode;
        content = pContent;
        data = pData;
    }

    /**
     * @return the id
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * @param pId
     *            the id to set
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
     * @param pContent
     *            the content to set
     */
    public void setContent(final String pContent) {
        content = pContent;
    }

    /**
     * @return the data
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param pData
     *            the data to set
     */
    public void setData(final Map<String, String> pData) {
        data = pData;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param pDescription
     *            the description to set
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

}
