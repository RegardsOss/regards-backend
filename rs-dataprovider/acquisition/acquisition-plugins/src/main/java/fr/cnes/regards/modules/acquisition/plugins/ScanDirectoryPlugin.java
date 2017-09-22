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

package fr.cnes.regards.modules.acquisition.plugins;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * Class ScanDirectoryPlugin A default {@link Plugin} of type {@link IConnectionPlugin}. Allows to
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "ScanDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ScanDirectoryPlugin implements IAcquisitionScanDirectoryPlugin {

    private static final String CHECKUM_ALGO = "SHA-256";
    
    public final static String META_PRODUCT_PARAM  = "meta-produt";
    
    public final static String META_FILE_PARAM  = "meta-file";

    @PluginParameter(name = META_PRODUCT_PARAM)
    MetaProduct metaProduct;

    @PluginParameter(name = META_FILE_PARAM)
    Set<MetaFile> metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {

        // TODO CMZ à compléter
        // pour chaque MetaFile
        // pour chaque ScanDirectory
        // tester date de dernière acquisition
        // chercher des fichiers vérifiant le pattern
        // créer des AcquisitionFile pour les fichiers trouvés

        Set<MetaFile> metas = getMetaFiles();

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        AcquisitionFile a = new AcquisitionFile();
        String aFileName = "Coucou";
        Product aProduct = ProductBuilder.build(aFileName).withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get();
        a.setProduct(aProduct);
        a.setFileName(aFileName);
        a.setSize(33L);
        a.setMetaFile(metas.iterator().next());
        a.setStatus(AcquisitionFileStatus.IN_PROGRESS);
        a.setAcqDate(OffsetDateTime.now());
        a.setAlgorithm(CHECKUM_ALGO);
        // a.setChecksum(null);
        acqFileList.add(a);

        AcquisitionFile b = new AcquisitionFile();
        String bFileName = "Hello Toulouse";
        Product bProduct = ProductBuilder.build(bFileName).withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get();
        b.setProduct(bProduct);
        b.setFileName(bFileName);
        b.setSize(156L);
        b.setMetaFile(metas.iterator().next());
        b.setStatus(AcquisitionFileStatus.IN_PROGRESS);
        b.setAcqDate(OffsetDateTime.now());
        b.setAlgorithm(CHECKUM_ALGO);
        // b.setChecksum(null);
        acqFileList.add(b);

        return acqFileList;
    }

    @Override
    public Set<MetaFile> getMetaFiles() {
        return metaFiles;
    }

}
