package fr.cnes.regards.modules.processing.entity;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_rights_plugin_configuration")
public class RightsPluginConfiguration {

    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "plugin_configuration_id", foreignKey = @ForeignKey(name = "fk_rights_plugin_configuration"))
    private PluginConfiguration pluginConfig;

    @Column(name = "tenant")
    private String tenant;

    @Column(name = "user_role", columnDefinition = "text")
    private String role;

    @Column(name = "datasets", columnDefinition = "int8[]")
    private List<Long> datasets;

}
