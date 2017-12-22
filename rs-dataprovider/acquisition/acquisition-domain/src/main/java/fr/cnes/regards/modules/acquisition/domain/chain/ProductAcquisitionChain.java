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

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;

/**
 *
 * TODO
 * @author Marc Sordi
 *
 */
public class ProductAcquisitionChain {

    // FIXME : on fixe, inutile de demander l'avis de l'exploitant
    private static final String CHECKSUM_ALGORITHM = "MD5";

    @NotBlank
    @Column(name = "label", length = 64, nullable = false)
    private String label;

    @NotNull
    @Column(name = "cleanOriginalFile")
    private final Boolean cleanOriginalFile = Boolean.TRUE;

    @NotNull // FIXME à moins que la chaine par défaut soit acceptable
    @Column(name = "ingest_chain")
    private String ingestChain;

    /**
     * The {@link List} of {@link MetaFile} for this {@link MetaProduct}
     */
    @OneToMany(fetch = FetchType.LAZY)
    // TODO @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_meta_product_id"))
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

}
