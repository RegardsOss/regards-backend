/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Domain class representing a template.
 *
 * @author Xavier-Alexandre Brochard
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
    @Column(name = "code")
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
    @CollectionTable(name = "t_template_data",joinColumns = @JoinColumn(name = "template_id"),foreignKey = @ForeignKey(name = "fk_template_data_template_id"))
    private Map<String, String> dataStructure;

    /**
     * A subject if the template should be written to something with a subject, title...
     */
    @NotBlank
    private final String subject;

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
        dataStructure = new HashMap<>();
        dataStructure.put("name", "Defaultname");
        subject = "Default subject";
    }

    /**
     * @param pCode
     *            the code
     * @param pContent
     *            the content
     * @param pData
     *            the data
     * @param pSubject
     *            the subject if the template should be written to something with a subject or title (like an email)
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
     * @return the dataStructure
     */
    public Map<String, String> getDataStructure() {
        return dataStructure;
    }

    /**
     * @param pDataStructure
     *            the data structure to set
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

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

}
