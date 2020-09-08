package fr.cnes.regards.modules.processing.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This class decorates a PluginConfiguration, corresponding to a Process plugin,
 * with associated access rights for the corresponding process.
 *
 * It allows to determine that a given process is usable by a given user role,
 * and for a given list of datasets.
 */
@Data @NoArgsConstructor @AllArgsConstructor
@TypeDef(
        name = "string-array",
        typeClass = StringArrayType.class
)
@Entity
@Table(name = "t_rights_plugin_configuration")
@SequenceGenerator(name = "pluginRightsConfSequence", initialValue = 1, sequenceName = "seq_plugin_rights_conf")
public class RightsPluginConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginRightsConfSequence")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plugin_configuration_id", foreignKey = @ForeignKey(name = "fk_rights_plugin_configuration"))
    private PluginConfiguration pluginConfiguration;

    @Column(name = "process_business_id")
    private UUID processBusinessId;

    /** Redundant information which is however somewhat useful for filtering in certain cases. */
    @Column(name = "tenant")
    private String tenant;

    @Column(name = "user_role", columnDefinition = "text")
    private String role;

    @Column(name = "datasets", columnDefinition = "varchar(128)[]")
    @Type(type = "string-array")
    private String[] datasets;

    public List<String> getDatasets() {
        return Arrays.asList(datasets);
    }
}
