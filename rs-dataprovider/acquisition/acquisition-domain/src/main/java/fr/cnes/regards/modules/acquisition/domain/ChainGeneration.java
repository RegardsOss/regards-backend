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
package fr.cnes.regards.modules.acquisition.domain;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;

/**
 * 
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_acquisition_chain", indexes = { @Index(name = "idx_acq_chain_label", columnList = "label") },
        uniqueConstraints = @UniqueConstraint(name = "uk_acq_chain_label", columnNames = { "label" }))
public class ChainGeneration implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 255
     */
    private static final int MAX_STRING_LENGTH = 255;

    /**
     * entity id for SGBD purpose mainly and REST request
     */
    @Id
    @SequenceGenerator(name = "ChainSequence", initialValue = 1, sequenceName = "seq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ChainSequence")
    private Long id;

    /**
     * Label to identify the {@link ChainGeneration}
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_LENGTH, nullable = false)
    private String label;

    /**
     * <code>true</code> if the {@link ChainGeneration} is active, <code>false</code> otherwise
     */
    @Column
    private Boolean active = false;

    /**
     * <code>true</code> if the {@link ChainGeneration} is currently running, <code>false</code> otherwise
     */
    @Column
    private Boolean running = false;

    /**
     * The last activation date when an acquisition were running 
     */
    @Column(name = "last_date_activation")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastDateActivation;

    /**
     * The periodicity in seconds between to acquisition
     */
    @Column(name = "period")
    private Long periodicity;

    /**
     * THe {@link MetaProduct} used for this {@link ChainGeneration}
     */
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_metaproduct_id"), nullable = true,
            updatable = true)
    private MetaProduct metaProduct;

    /**
     * The ipId of the dataset for which the acquired files are set
     */
    @Column(length = MAX_STRING_LENGTH)
    private String dataSetIpId;

    /**
     * If a {@link ChainGeneration} is running, the current session identifier must be defined and unique
     */
    @Column(length = MAX_STRING_LENGTH)
    private String session;

    /**
     * A comment
     */
    @Column(name = "comment")
    @Type(type = "text")
    private String comment;

    /**
     * A {@link PluginConfiguration} of a {@link IAcquisitionScanPlugin}
     */
    @ManyToOne
    @JoinColumn(name = "scan_conf_id", foreignKey = @ForeignKey(name = "fk_scan_conf_id"))
    private PluginConfiguration scanAcquisitionPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link ICheckFilePlugin}
     */
    @ManyToOne
    @JoinColumn(name = "checkfile_conf_id", foreignKey = @ForeignKey(name = "fk_checkfile_conf_id"))
    private PluginConfiguration checkAcquisitionPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IGenerateSIPPlugin}
     */
    @ManyToOne
    @JoinColumn(name = "generatesip_conf_id", foreignKey = @ForeignKey(name = "fk_generatesip_conf_id"))
    private PluginConfiguration generateSipPluginConf;

    /**
     * A {@link PluginConfiguration} of a {@link IPostProcessSipPlugin}
     */
    @ManyToOne
    @JoinColumn(name = "postprocesssip_conf_id", foreignKey = @ForeignKey(name = "fk_postprocesssip_conf_id"))
    private PluginConfiguration postProcessSipPluginConf;

    @Transient
    private final Map<String, String> scanAcquisitionParameter = new HashMap<>();

    @Transient
    private final Map<String, String> checkAcquisitionParameter = new HashMap<>();

    @Transient
    private final Map<String, String> generateSipParameter = new HashMap<>();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataSetIpId == null) ? 0 : dataSetIpId.hashCode()); // NOSONAR
        result = prime * result + ((label == null) ? 0 : label.hashCode()); // NOSONAR
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChainGeneration other = (ChainGeneration) obj;
        if (dataSetIpId == null) {
            if (other.dataSetIpId != null) {
                return false;
            }
        } else if (!dataSetIpId.equals(other.dataSetIpId)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        return true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public OffsetDateTime getLastDateActivation() {
        return lastDateActivation;
    }

    public void setLastDateActivation(OffsetDateTime lastDateActivation) {
        this.lastDateActivation = lastDateActivation;
    }

    public Long getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Long periodicity) {
        this.periodicity = periodicity;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    public String getDataSet() {
        return dataSetIpId;
    }

    public void setDataSet(String dataSet) {
        this.dataSetIpId = dataSet;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PluginConfiguration getScanAcquisitionPluginConf() {
        return scanAcquisitionPluginConf;
    }

    public void setScanAcquisitionPluginConf(PluginConfiguration scanAcquisitionPluginConf) {
        this.scanAcquisitionPluginConf = scanAcquisitionPluginConf;
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

    public void setGenerateSipPluginConf(PluginConfiguration generateSIPPluginConf) {
        this.generateSipPluginConf = generateSIPPluginConf;
    }

    public PluginConfiguration getPostProcessSipPluginConf() {
        return postProcessSipPluginConf;
    }

    public void setPostProcessSipPluginConf(PluginConfiguration postProcessSipPluginConf) {
        this.postProcessSipPluginConf = postProcessSipPluginConf;
    }

    public Map<String, String> getScanAcquisitionParameter() {
        return scanAcquisitionParameter;
    }

    public void addScanAcquisitionParameter(String name, String value) {
        this.scanAcquisitionParameter.put(name, value);
    }

    public Map<String, String> getCheckAcquisitionParameter() {
        return checkAcquisitionParameter;
    }

    public void addCheckAcquisitionParameter(String name, String value) {
        this.checkAcquisitionParameter.put(name, value);
    }

    public Map<String, String> getGenerateSipParameter() {
        return generateSipParameter;
    }

    public void addGenerateSipParameter(String name, String value) {
        this.generateSipParameter.put(name, value);
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(label);
        strBuilder.append(" - active=");
        strBuilder.append(active.toString());
        strBuilder.append(" - dataset=");
        strBuilder.append(dataSetIpId);
        strBuilder.append(" - session=");
        strBuilder.append(session);
        strBuilder.append(" - [");
        strBuilder.append(metaProduct.toString());
        strBuilder.append("]");
        return strBuilder.toString();
    }

}
