/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.validation.PluginServices;

/**
 * Class mapping Plugins of type IFilter, IConverter, IService to a Dataset
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_link_dataset")
public class LinkPluginsDatasets {

    /**
     * Id of the dataset which is concerned by this mapping
     */
    @Id
    private Long datasetId;

    /**
     * Ids of plugin configuration of type IService
     */
    @ManyToMany
    @JoinTable(name = "ta_link_dataset_services", joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "service_configuration_id"))
    private Set<PluginConfiguration> services;

    protected LinkPluginsDatasets() {
        // only there for (de)serialization purpose and hibernate
    }

    public LinkPluginsDatasets(Long pDatasetId, @PluginServices Set<PluginConfiguration> pServices) {
        super();
        datasetId = pDatasetId;
        services = pServices;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long pDatasetId) {
        datasetId = pDatasetId;
    }

    public Set<PluginConfiguration> getServices() {
        return services;
    }

    public void setServices(@PluginServices Set<PluginConfiguration> pServices) {
        services = pServices;
    }

}
