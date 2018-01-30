/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain.chain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;

/**
 *
 * Define a product acquisition chain
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_acq_processing_chain")
@NamedEntityGraphs({ @NamedEntityGraph(name = "graph.acquisition.file.info.complete",
        attributeNodes = @NamedAttributeNode(value = "fileInfos")) })
public class AcquisitionProcessingChain {

    /**
     * Fixed checksum algorithm
     */
    public static final String CHECKSUM_ALGORITHM = "MD5";

    @Id
    @SequenceGenerator(name = "AcquisitionChainSequence", initialValue = 1, sequenceName = "seq_acq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcquisitionChainSequence")
    private Long id;

    @NotBlank(message = "Processing chain label is required")
    @Column(name = "label", length = 64, nullable = false)
    private String label;

    /**
     * <code>true</code> if active, <code>false</code> otherwise
     */
    @NotNull(message = "Processing chain state is required")
    @Column
    private Boolean active = false;

    @NotNull(message = "Acquisition processing mode is required")
    @Column(length = 16)
    @Enumerated(EnumType.STRING)
    private AcquisitionProcessingChainMode mode = AcquisitionProcessingChainMode.AUTO;

    // FIXME @Max(value = 50, message = "Session must be 50 characters length max")
    @Column(length = 50)
    private String session;

    /**
     * <code>true</code> if currently running, <code>false</code>
     * otherwise.<br/>
     * The same acquisition chain must not be run twice!
     */
    @Column(updatable = false, nullable = false)
    private Boolean running = false;

    /**
     * The last activation date when an acquisition were running.
     */
    @Column(name = "last_activation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastActivationDate;

    /**
     * The periodicity in seconds between two acquisitions
     */
    @Column(name = "period")
    @Min(value = 10, message = "Periodicity must be greater or equals to 10 seconds")
    private Long periodicity;

    /**
     * Then INGEST chain name for SIP submission
     */
    @NotBlank(message = "Ingest chain is required")
    @Column(name = "ingest_chain")
    private String ingestChain;

    /**
     * The ipId of the dataset for which the acquired files are set
     */
    @NotBlank(message = "Dataset IP ID is required")
    @Column(length = 256)
    private String datasetIpId;

    /**
     * The {@link List} of files to build a product
     */
    @NotNull(message = "A processing chain must have at least one acquisition file information")
    @Size(min = 1)
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "acq_chain_id", foreignKey = @ForeignKey(name = "fk_acq_chain_id"))
    private List<AcquisitionFileInfo> fileInfos;

    /**
     * An optional {@link PluginConfiguration} of a {@link IValidationPlugin}
     */
    @NotNull(message = "Validation plugin configuration is required")
    @ManyToOne
    @JoinColumn(name = "validation_conf_id", foreignKey = @ForeignKey(name = "fk_validation_conf_id"))
    private PluginConfiguration validationPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IProductPlugin}
     */
    @NotNull(message = "Product plugin configuration is required")
    @ManyToOne
    @JoinColumn(name = "product_conf_id", foreignKey = @ForeignKey(name = "fk_product_conf_id"))
    private PluginConfiguration productPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link ISipGenerationPlugin}
     */
    @NotNull(message = "SIP generation plugin configuration is required")
    @ManyToOne
    @JoinColumn(name = "generatesip_conf_id", foreignKey = @ForeignKey(name = "fk_generatesip_conf_id"))
    private PluginConfiguration generateSipPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link ISipPostProcessingPlugin}
     */
    @ManyToOne
    @JoinColumn(name = "postprocesssip_conf_id", foreignKey = @ForeignKey(name = "fk_postprocesssip_conf_id"))
    private PluginConfiguration postProcessSipPluginConf;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getLastActivationDate() {
        return lastActivationDate;
    }

    public void setLastActivationDate(OffsetDateTime lastActivationDate) {
        this.lastActivationDate = lastActivationDate;
    }

    public Long getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Long periodicity) {
        this.periodicity = periodicity;
    }

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
    }

    public String getDatasetIpId() {
        return datasetIpId;
    }

    public void setDatasetIpId(String datasetIpId) {
        this.datasetIpId = datasetIpId;
    }

    public List<AcquisitionFileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(List<AcquisitionFileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }

    public void addFileInfo(AcquisitionFileInfo fileInfo) {
        if (fileInfos == null) {
            fileInfos = new ArrayList<>();
        }
        fileInfos.add(fileInfo);
    }

    public PluginConfiguration getGenerateSipPluginConf() {
        return generateSipPluginConf;
    }

    public void setGenerateSipPluginConf(PluginConfiguration generateSipPluginConf) {
        this.generateSipPluginConf = generateSipPluginConf;
    }

    public Optional<PluginConfiguration> getPostProcessSipPluginConf() {
        return Optional.ofNullable(postProcessSipPluginConf);
    }

    public void setPostProcessSipPluginConf(PluginConfiguration postProcessSipPluginConf) {
        this.postProcessSipPluginConf = postProcessSipPluginConf;
    }

    public boolean isActive() {
        return active;
    }

    public AcquisitionProcessingChainMode getMode() {
        return mode;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PluginConfiguration getValidationPluginConf() {
        return validationPluginConf;
    }

    public void setValidationPluginConf(PluginConfiguration validationPluginConf) {
        this.validationPluginConf = validationPluginConf;
    }

    public PluginConfiguration getProductPluginConf() {
        return productPluginConf;
    }

    public void setProductPluginConf(PluginConfiguration productPluginConf) {
        this.productPluginConf = productPluginConf;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setMode(AcquisitionProcessingChainMode mode) {
        this.mode = mode;
    }

    public Optional<String> getSession() {
        return Optional.ofNullable(session);
    }

    public void setSession(String session) {
        this.session = session;
    }
}
