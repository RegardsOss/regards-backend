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
import java.util.List;
import java.util.Set;

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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;

/**
 *
 * Define a product acquisition chain
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_acq_processing_chain",
        uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_chain_name", columnNames = "name") })
public class AcquisitionProcessingChain {

    /**
     * Fixed checksum algorithm
     */
    public static final String CHECKSUM_ALGORITHM = "MD5";

    @Id
    @SequenceGenerator(name = "AcquisitionChainSequence", initialValue = 1, sequenceName = "seq_acq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcquisitionChainSequence")
    private Long id;

    @NotBlank
    @Column(name = "label", length = 64, nullable = false)
    private String label;

    /**
     * <code>true</code> if active, <code>false</code> otherwise
     */
    @NotNull
    @Column
    private final Boolean active = false;

    @NotNull(message = "Acquisition processing mode is required")
    @Column(length = 16)
    @Enumerated(EnumType.STRING)
    private final AcquisitionProcessingChainMode mode = AcquisitionProcessingChainMode.AUTO;

    /**
     * <code>true</code> if currently running, <code>false</code>
     * otherwise.<br/>
     * The same acquisition chain must not be run twice!
     */
    @Column
    private final Boolean running = false;

    /**
     * The last activation date when an acquisition were running
     */
    @Column(name = "last_activation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastActivationDate;

    /**
     * The periodicity in seconds between two acquisitions
     */
    @Column(name = "period")
    private Long periodicity;

    /**
     * Then INGEST chain name for SIP submission
     */
    @NotNull
    @Column(name = "ingest_chain")
    private String ingestChain;

    /**
     * The ipId of the dataset for which the acquired files are set
     */
    @Column(length = 256)
    private String datasetIpId;

    /**
     * The {@link List} of files to build a product
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "acq_chain_id", foreignKey = @ForeignKey(name = "fk_acq_chain_id"))
    private Set<AcquisitionFileInfo> fileInfos;

    /**
     * A {@link PluginConfiguration} of a {@link ICheckFilePlugin}
     */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "checkfile_conf_id", foreignKey = @ForeignKey(name = "fk_checkfile_conf_id"))
    private PluginConfiguration checkAcquisitionPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IGenerateSIPPlugin}
     */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "generatesip_conf_id", foreignKey = @ForeignKey(name = "fk_generatesip_conf_id"))
    private PluginConfiguration generateSipPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IPostProcessSipPlugin}
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

    public Set<AcquisitionFileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(Set<AcquisitionFileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }

    public PluginConfiguration getCheckAcquisitionPluginConf() {
        return checkAcquisitionPluginConf;
    }

    public void setCheckAcquisitionPluginConf(PluginConfiguration checkAcquisitionPluginConf) {
        this.checkAcquisitionPluginConf = checkAcquisitionPluginConf;
    }

    public PluginConfiguration getGenerateSipPluginConf() {
        return generateSipPluginConf;
    }

    public void setGenerateSipPluginConf(PluginConfiguration generateSipPluginConf) {
        this.generateSipPluginConf = generateSipPluginConf;
    }

    public PluginConfiguration getPostProcessSipPluginConf() {
        return postProcessSipPluginConf;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
