/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * Class Plugin
 *
 * Entity to describe an IHM Plugin definition.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "T_IHM_PLUGINS")
public class UIPluginDefinition {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmPluginsSequence", initialValue = 1, sequenceName = "SEQ_IHM_PLUGINS")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmPluginsSequence")
    private Long id;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    private UIPluginTypesEnum type;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    private String sourcePath;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public UIPluginTypesEnum getType() {
        return type;
    }

    public void setType(final UIPluginTypesEnum pType) {
        type = pType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(final String pSourcePath) {
        sourcePath = pSourcePath;
    }

}
