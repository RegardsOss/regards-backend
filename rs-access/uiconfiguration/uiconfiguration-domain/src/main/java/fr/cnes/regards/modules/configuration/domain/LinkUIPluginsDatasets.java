/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.*;
import java.util.List;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;

/**
 * Class to link a dataset to a UIPuginConfigurations
 *
 * @author Sébastien Binda
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_link_uiservice_dataset",
        uniqueConstraints = @UniqueConstraint(name="uk_link_uiservice_dataset_dataset_id", columnNames = "dataset_id"))
@NamedEntityGraph(name = "graph.link.configurations", attributeNodes = @NamedAttributeNode(value = "services"))
public class LinkUIPluginsDatasets {

    /**
     * Id of the dataset which is concerned by this mapping
     */
    @Id
    @SequenceGenerator(name = "ihmLinkUiPluginDatasetSequence", initialValue = 1,
            sequenceName = "seq_ihm_uiplugin_dataset")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmLinkUiPluginDatasetSequence")
    private Long linkId;

    @Column(name = "dataset_id", length = 256)
    private String datasetId;

    /**
     * Ids of plugin configuration of type IService
     */
    @ManyToMany
    @JoinTable(name = "ta_link_dataset_uiservices", joinColumns = @JoinColumn(name = "dataset_id", foreignKey = @ForeignKey(name = "fk_link_dataset_uiservices_dataset_id_service_configuration_id")),
            inverseJoinColumns = @JoinColumn(name = "service_configuration_id", foreignKey = @ForeignKey(name = "fk_link_dataset_uiservices_service_configuration_id_dataset_id")))
    private List<UIPluginConfiguration> services;

    /**
     * Only there for (de)serialization purpose and hibernate
     */
    public LinkUIPluginsDatasets() {
    }

    /**
     * Constructor
     *
     * @param pDatasetId
     *            Id of the dataset which is concerned by this mapping
     * @param pServices
     *            Ids of plugin configuration of type IService
     */
    public LinkUIPluginsDatasets(final String pDatasetId, final List<UIPluginConfiguration> pServices) {
        super();
        datasetId = pDatasetId;
        services = pServices;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(final String pDatasetId) {
        datasetId = pDatasetId;
    }

    public List<UIPluginConfiguration> getServices() {
        return services;
    }

    public void setServices(final List<UIPluginConfiguration> pServices) {
        services = pServices;
    }

}
