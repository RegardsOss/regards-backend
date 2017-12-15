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
import java.util.Optional;

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
import javax.validation.constraints.NotNull;

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
public class AcquisitionProcessingChain implements IIdentifiable<Long> {

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
     * Label to identify the {@link AcquisitionProcessingChain}
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_LENGTH, nullable = false)
    private String label;

    /**
     * <code>true</code> if the {@link AcquisitionProcessingChain} is active, <code>false</code> otherwise
     */
    @Column
    private Boolean active = false;

    /**
     * <code>true</code> if the {@link AcquisitionProcessingChain} is currently running, <code>false</code> otherwise
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
     * The {@link MetaProduct} used for this {@link AcquisitionProcessingChain}
     */
    @NotNull
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
     * If a {@link AcquisitionProcessingChain} is running, the current session identifier must be defined and unique
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
    @NotNull
    @ManyToOne
    @JoinColumn(name = "scan_conf_id", foreignKey = @ForeignKey(name = "fk_scan_conf_id"))
    private PluginConfiguration scanAcquisitionPluginConf;

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

    // FIXME à virer
    @Transient
    private final Map<String, String> scanAcquisitionParameter = new HashMap<>();

    // FIXME à virer
    @Transient
    private final Map<String, String> checkAcquisitionParameter = new HashMap<>();

    // FIXME à virer
    @Transient
    private final Map<String, String> generateSipParameter = new HashMap<>();

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

    public Optional<PluginConfiguration> getPostProcessSipPluginConf() {
        return Optional.ofNullable(postProcessSipPluginConf);
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

    @Override
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((active == null) ? 0 : active.hashCode());
        result = (prime * result) + ((checkAcquisitionParameter == null) ? 0 : checkAcquisitionParameter.hashCode());
        result = (prime * result) + ((checkAcquisitionPluginConf == null) ? 0 : checkAcquisitionPluginConf.hashCode());
        result = (prime * result) + ((comment == null) ? 0 : comment.hashCode());
        result = (prime * result) + ((dataSetIpId == null) ? 0 : dataSetIpId.hashCode());
        result = (prime * result) + ((generateSipParameter == null) ? 0 : generateSipParameter.hashCode());
        result = (prime * result) + ((generateSipPluginConf == null) ? 0 : generateSipPluginConf.hashCode());
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        result = (prime * result) + ((label == null) ? 0 : label.hashCode());
        result = (prime * result) + ((lastDateActivation == null) ? 0 : lastDateActivation.hashCode());
        result = (prime * result) + ((metaProduct == null) ? 0 : metaProduct.hashCode());
        result = (prime * result) + ((periodicity == null) ? 0 : periodicity.hashCode());
        result = (prime * result) + ((postProcessSipPluginConf == null) ? 0 : postProcessSipPluginConf.hashCode());
        result = (prime * result) + ((running == null) ? 0 : running.hashCode());
        result = (prime * result) + ((scanAcquisitionParameter == null) ? 0 : scanAcquisitionParameter.hashCode());
        result = (prime * result) + ((scanAcquisitionPluginConf == null) ? 0 : scanAcquisitionPluginConf.hashCode());
        result = (prime * result) + ((session == null) ? 0 : session.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AcquisitionProcessingChain other = (AcquisitionProcessingChain) obj;
        if (active == null) {
            if (other.active != null) {
                return false;
            }
        } else if (!active.equals(other.active)) {
            return false;
        }
        if (checkAcquisitionParameter == null) {
            if (other.checkAcquisitionParameter != null) {
                return false;
            }
        } else if (!checkAcquisitionParameter.equals(other.checkAcquisitionParameter)) {
            return false;
        }
        if (checkAcquisitionPluginConf == null) {
            if (other.checkAcquisitionPluginConf != null) {
                return false;
            }
        } else if (!checkAcquisitionPluginConf.equals(other.checkAcquisitionPluginConf)) {
            return false;
        }
        if (comment == null) {
            if (other.comment != null) {
                return false;
            }
        } else if (!comment.equals(other.comment)) {
            return false;
        }
        if (dataSetIpId == null) {
            if (other.dataSetIpId != null) {
                return false;
            }
        } else if (!dataSetIpId.equals(other.dataSetIpId)) {
            return false;
        }
        if (generateSipParameter == null) {
            if (other.generateSipParameter != null) {
                return false;
            }
        } else if (!generateSipParameter.equals(other.generateSipParameter)) {
            return false;
        }
        if (generateSipPluginConf == null) {
            if (other.generateSipPluginConf != null) {
                return false;
            }
        } else if (!generateSipPluginConf.equals(other.generateSipPluginConf)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (lastDateActivation == null) {
            if (other.lastDateActivation != null) {
                return false;
            }
        } else if (!lastDateActivation.equals(other.lastDateActivation)) {
            return false;
        }
        if (metaProduct == null) {
            if (other.metaProduct != null) {
                return false;
            }
        } else if (!metaProduct.equals(other.metaProduct)) {
            return false;
        }
        if (periodicity == null) {
            if (other.periodicity != null) {
                return false;
            }
        } else if (!periodicity.equals(other.periodicity)) {
            return false;
        }
        if (postProcessSipPluginConf == null) {
            if (other.postProcessSipPluginConf != null) {
                return false;
            }
        } else if (!postProcessSipPluginConf.equals(other.postProcessSipPluginConf)) {
            return false;
        }
        if (running == null) {
            if (other.running != null) {
                return false;
            }
        } else if (!running.equals(other.running)) {
            return false;
        }
        if (scanAcquisitionParameter == null) {
            if (other.scanAcquisitionParameter != null) {
                return false;
            }
        } else if (!scanAcquisitionParameter.equals(other.scanAcquisitionParameter)) {
            return false;
        }
        if (scanAcquisitionPluginConf == null) {
            if (other.scanAcquisitionPluginConf != null) {
                return false;
            }
        } else if (!scanAcquisitionPluginConf.equals(other.scanAcquisitionPluginConf)) {
            return false;
        }
        if (session == null) {
            if (other.session != null) {
                return false;
            }
        } else if (!session.equals(other.session)) {
            return false;
        }
        return true;
    }
}
