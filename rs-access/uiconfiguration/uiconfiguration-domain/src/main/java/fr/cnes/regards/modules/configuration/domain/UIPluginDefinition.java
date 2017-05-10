/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(name = "t_ui_plugin")
public class UIPluginDefinition {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmPluginsSequence", initialValue = 1, sequenceName = "seq_ui_plugin")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmPluginsSequence")
    private Long id;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false, length = 32)
    private String name;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UIPluginDefinition other = (UIPluginDefinition) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else
            if (!id.equals(other.id)) {
                return false;
            }
        return true;
    }

}
