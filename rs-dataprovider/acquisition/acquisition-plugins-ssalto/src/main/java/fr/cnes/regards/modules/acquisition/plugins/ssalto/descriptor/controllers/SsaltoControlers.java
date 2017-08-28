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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.ClobAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.DateAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.GeoAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.LongAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.RealAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.StringAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.UrlAttribute;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectUpdateElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataStorageObjectElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorDateAttribute;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorDateTimeAttribute;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDeletionDescriptorElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * @author Christophe Mertz
 */
public class SsaltoControlers {

    /*
     * DataObjectElement controlers
     */
    private static DataObjectDescriptionElementControler dataObjectDescriptionElementControler = new DataObjectDescriptionElementControler();

    private static DataObjectUpdateElementControler dataObjectUpdateElementControler = new DataObjectUpdateElementControler();

    //    /*
    //     * DataStorageObjectElement controlers
    //     */
    //    private static DataStorageObjectDescriptionElementControler dataStorageObjectDescriptionElementControler_ = new DataStorageObjectDescriptionElementControler();
    //
    //    private static DataStorageObjectUpdateElementControler dataStorageObjectUpdateElementControler_ = new DataStorageObjectUpdateElementControler();

    /*
     * DescriptorFileControler controlers
     */
    //    private static DeletionDescriptorFileControler deletionDescriptorFileControler_ = new DeletionDescriptorFileControler();

    //    private static DescriptorFileControler descriptorFileControler_ = new DescriptorFileControler();

    /*
     * EntityDescriptorElement controlers
     */
    private static EntityDeletionDescriptorElementControler entityDeletionDescriptorElementControler = new EntityDeletionDescriptorElementControler();

    private static ClobAttributeControler clobAttributeControler = new ClobAttributeControler();

    private static GeoAttributeControler geoAttributeControler = new GeoAttributeControler();

    private static RealAttributeControler realAttributeControler = new RealAttributeControler();

    private static LongAttributeControler longAttributeControler = new LongAttributeControler();

    private static StringAttributeControler stringAttributeControler = new StringAttributeControler();

    private static UrlAttributeControler urlAttributeControler = new UrlAttributeControler();

    private static DescriptorDateAttributeControler descriptorDateAttributeControler = new DescriptorDateAttributeControler();

    private static DateAttributeControler dateAttributeControler = new DateAttributeControler();

    private static DescriptorDateTimeAttributeControler descriptorDateTimeAttributeControler = new DescriptorDateTimeAttributeControler();

    private static DateTimeAttributeControler dateTimeAttributeControler = new DateTimeAttributeControler();

    //    /*
    //     * AbstractDomainObject controlers
    //     */
    //    private static DescriptorElementControler descriptorElementControler_ = new DescriptorElementControler();
    //
    //    private static PhysicalFileControler physicalFileControler_ = new PhysicalFileControler();
    //
    //    private static CutPhysicalFileControler cutPhysicalFileControler_ = new CutPhysicalFileControler();
    //
    //    private static STAFProjectNodeControler sTAFProjectNodeControler_ = new STAFProjectNodeControler();
    //
    //    private static TarPhysicalFileControler tarPhysicalFileControler_ = new TarPhysicalFileControler();
    //
    //    private static ArchivingFolderControler archivingFolderControler_ = new ArchivingFolderControler();
    //
    //    private static MetaFileControler metaFileControler_ = new MetaFileControler();
    //
    //    private static MetaProductAcquisitionInfosControler metaProductAcquisitionInfosControler_ = new MetaProductAcquisitionInfosControler();
    //
    //    private static MetaProductArchivingInfosControler metaProductArchivingInfosControler_ = new MetaProductArchivingInfosControler();
    //
    //    private static MetaProductControler metaProductControler_ = new MetaProductControler();
    //
    //    private static STAFProjectControler sTAFProjectControler_ = new STAFProjectControler();
    //
    //    private static SupplyControler supplyControler_ = new SupplyControler();
    //
    //    private static SupplyDirectoryControler supplyDirectoryControler_ = new SupplyDirectoryControler();
    //
    //    private static TransformerControler transformerControler_ = new TransformerControler();
    //
    //    private static PostAcquisitionPluginChainControler postAcquisitionPluginChainControler_ = new PostAcquisitionPluginChainControler();
    //
    //    private static ProductControler productControler_ = new ProductControler();
    //
    //    /*
    //     * LocalPhysicalLocation controlers
    //     */
    //    private static LocalPhysicalLocationControler localPhysicalLocationControler_ = new LocalPhysicalLocationControler();
    //
    //    /*
    //     * EntityDescriptorElement controlers
    //     */
    //    private static SingleSTAFPhysicalLocationControler singleSTAFPhysicalLocationControler_ = new SingleSTAFPhysicalLocationControler();
    //
    //    private static MultipleSTAFPhysicalLocationControler multipleSTAFPhysicalLocationControler_ = new MultipleSTAFPhysicalLocationControler();
    //
    //    /*
    //     * AbstractProcessInformations controlers
    //     */
    //    private static AcquisitionProcessInformationsControler acquisitionProcessInformationsControler_ = new AcquisitionProcessInformationsControler();
    //
    //    private static ArchiveCleaningProcessInformationsControler archiveCleaningProcessInformationsControler_ = new ArchiveCleaningProcessInformationsControler();
    //
    //    private static ArchivingProcessInformationsControler archivingProcessInformationsControler_ = new ArchivingProcessInformationsControler();
    //
    //    private static CatalogueUpdateProcessInformationsControler catalogueUpdateProcessInformationsControler_ = new CatalogueUpdateProcessInformationsControler();
    //
    //    private static DeletionProcessInformationsControler deletionProcessInformationsControler_ = new DeletionProcessInformationsControler();
    //
    //    /*
    //     * ProcessStatus controlers
    //     */
    //    private static ProcessStatusControler processStatusControler_ = new ProcessStatusControler();
    //
    //    /*
    //     * FilePlugin controlers
    //     */
    //    private static FileCheckingPluginControler fileCheckingPluginControler_ = new FileCheckingPluginControler();
    //
    //    private static FileMetaDataCreationPluginControler fileMetaDataCreationPluginControler_ = new FileMetaDataCreationPluginControler();
    //
    //    /*
    //     * ProductPlugin controlers
    //     */
    //    private static PostAcquisitionPluginControler postAcquisitionPluginControler_ = new PostAcquisitionPluginControler();
    //
    //    private static ProductMetaDataCreationPluginControler productMetaDataCreationPluginControler_ = new ProductMetaDataCreationPluginControler();
    //
    /*
     * Attribute controlers
     */
    private static CompositeAttributeControler compositeAttributeControler_ = new CompositeAttributeControler();
    //
    //    /*
    //     * Ssalto File controlers
    //     */
    //
    //    private static SsaltoFileControler ssaltoFileControler_ = new SsaltoFileControler();
    //
    //    private static SsaltoFileStatusControler ssaltoFileStatusControler_ = new SsaltoFileStatusControler();
    //
    //    /*
    //     * File process controlers
    //     */
    //    private static FileAcquisitionInformationsControler fileAcquisitionInformationsControler_ = new FileAcquisitionInformationsControler();
    //
    //    private static FileArchivingInformationsControler fileArchivingInformationsControler_ = new FileArchivingInformationsControler();
    //
    //    private static FileCleaningInformationsControler fileCleaningInformationsControler_ = new FileCleaningInformationsControler();
    //
    //    private static FileDeletionInformationsControler fileDeletionInformationsControler_ = new FileDeletionInformationsControler();

    /**
     * @param pDataObjectElement
     * @return
     * @since 5.2
     */
    public static DataObjectElementControler getControler(DataObjectElement pDataObjectElement) {
        if (pDataObjectElement instanceof DataObjectDescriptionElement) {
            return dataObjectDescriptionElementControler;
        }
        if (pDataObjectElement instanceof DataObjectUpdateElement) {
            return dataObjectUpdateElementControler;
        }
        return null;
    }
    //
    //    /**
    //     * @param pDataStorageObjectElement
    //     * @return
    //     * @since 5.2
    //     */
    //    public static DataStorageObjectElementControler getControler(DataStorageObjectElement pDataStorageObjectElement) {
    //        if (pDataStorageObjectElement instanceof DataStorageObjectDescriptionElement) {
    //            return dataStorageObjectDescriptionElementControler_;
    //        }
    //
    //        if (pDataStorageObjectElement instanceof DataStorageObjectUpdateElement) {
    //            return dataStorageObjectUpdateElementControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pDataStorageObjectElement
    //     * @return
    //     * @since 5.2
    //     */
    //    public static DescriptorFileControler getControler(DescriptorFile pDescriptorFile) {
    //        if (pDescriptorFile instanceof DeletionDescriptorFile) {
    //            return deletionDescriptorFileControler_;
    //        }
    //        if (pDescriptorFile instanceof DescriptorFile) {
    //            return descriptorFileControler_;
    //        }
    //        return null;
    //    }

    /**
     * @param pEntityDescriptorElement
     * @return
     * @since 5.2
     */
    public static EntityDescriptorElementControler getControler(EntityDescriptorElement pEntityDescriptorElement) {
        if (pEntityDescriptorElement instanceof DataObjectElement) {
            return getControler((DataObjectElement) pEntityDescriptorElement);
        }
        if (pEntityDescriptorElement instanceof DataStorageObjectElement) {
            return getControler((DataStorageObjectElement) pEntityDescriptorElement);
        }
        if (pEntityDescriptorElement instanceof EntityDeletionDescriptorElement) {
            return entityDeletionDescriptorElementControler;
        }
        return null;
    }

    //    /**
    //     * @param pAbstractDomainObject
    //     * @return
    //     * @since 5.2
    //     */
    //    public static AbstractDomainObjectControler getControler(AbstractDomainObject pAbstractDomainObject) {
    //        if (pAbstractDomainObject instanceof DescriptorElement) {
    //            return descriptorElementControler_;
    //        }
    //        if (pAbstractDomainObject instanceof PhysicalFile) {
    //            return physicalFileControler_;
    //        }
    //        if (pAbstractDomainObject instanceof CutPhysicalFile) {
    //            return cutPhysicalFileControler_;
    //        }
    //        if (pAbstractDomainObject instanceof STAFProjectNode) {
    //            return sTAFProjectNodeControler_;
    //        }
    //        if (pAbstractDomainObject instanceof TarPhysicalFile) {
    //            return tarPhysicalFileControler_;
    //        }
    //        if (pAbstractDomainObject instanceof ArchivingFolder) {
    //            return archivingFolderControler_;
    //        }
    //        if (pAbstractDomainObject instanceof MetaFile) {
    //            return metaFileControler_;
    //        }
    //        if (pAbstractDomainObject instanceof MetaProductAcquisitionInfos) {
    //            return metaProductAcquisitionInfosControler_;
    //        }
    //        if (pAbstractDomainObject instanceof MetaProductArchivingInfos) {
    //            return metaProductArchivingInfosControler_;
    //        }
    //        if (pAbstractDomainObject instanceof MetaProduct) {
    //            return metaProductControler_;
    //        }
    //        if (pAbstractDomainObject instanceof STAFProject) {
    //            return sTAFProjectControler_;
    //        }
    //        if (pAbstractDomainObject instanceof Supply) {
    //            return supplyControler_;
    //        }
    //        if (pAbstractDomainObject instanceof SupplyDirectory) {
    //            return supplyDirectoryControler_;
    //        }
    //        if (pAbstractDomainObject instanceof Transformer) {
    //            return transformerControler_;
    //        }
    //        if (pAbstractDomainObject instanceof PostAcquisitionPluginChain) {
    //            return postAcquisitionPluginChainControler_;
    //        }
    //        if (pAbstractDomainObject instanceof AbstractProcessInformations) {
    //            return getControler((AbstractProcessInformations) pAbstractDomainObject);
    //        }
    //        if (pAbstractDomainObject instanceof Plugin) {
    //            return getControler((Plugin) pAbstractDomainObject);
    //        }
    //        if (pAbstractDomainObject instanceof SsaltoFile) {
    //            return ssaltoFileControler_;
    //        }
    //        if (pAbstractDomainObject instanceof Product) {
    //            return productControler_;
    //        }
    //        if (pAbstractDomainObject instanceof SsaltoFileStatus) {
    //            return ssaltoFileStatusControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static FileAcquisitionInformationsControler getControler(
    //            FileAcquisitionInformations pFileAcquisitionInformations) {
    //        if (pFileAcquisitionInformations instanceof FileAcquisitionInformations) {
    //            return fileAcquisitionInformationsControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static FileArchivingInformationsControler getControler(FileArchivingInformations pFileArchivingInformations) {
    //        if (pFileArchivingInformations instanceof FileArchivingInformations) {
    //            return fileArchivingInformationsControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static FileCleaningInformationsControler getControler(FileCleaningInformations pFileCleaningInformations) {
    //        if (pFileCleaningInformations instanceof FileCleaningInformations) {
    //            return fileCleaningInformationsControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static FileDeletionInformationsControler getControler(FileDeletionInformations pFileDeletionInformations) {
    //        if (pFileDeletionInformations instanceof FileDeletionInformations) {
    //            return fileDeletionInformationsControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pLocalPhysicalLocation
    //     * @return
    //     * @since 5.2
    //     */
    //    public static LocalPhysicalLocationControler getControler(LocalPhysicalLocation pLocalPhysicalLocation) {
    //        if (pLocalPhysicalLocation instanceof LocalPhysicalLocation) {
    //            return localPhysicalLocationControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pDataStorageObjectElement
    //     * @return
    //     * @since 5.2
    //     */
    //    public static STAFPhysicalLocationControler getControler(STAFPhysicalLocation pSTAFPhysicalLocation) {
    //        if (pSTAFPhysicalLocation instanceof SingleSTAFPhysicalLocation) {
    //            return singleSTAFPhysicalLocationControler_;
    //        }
    //        if (pSTAFPhysicalLocation instanceof MultipleSTAFPhysicalLocation) {
    //            return multipleSTAFPhysicalLocationControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pAbstractProcessInformations
    //     * @return
    //     * @since 5.2
    //     */
    //    public static AbstractProcessInformationsControler getControler(
    //            AbstractProcessInformations pAbstractProcessInformations) {
    //        if (pAbstractProcessInformations instanceof AcquisitionProcessInformations) {
    //            return acquisitionProcessInformationsControler_;
    //        }
    //        if (pAbstractProcessInformations instanceof ArchiveCleaningProcessInformations) {
    //            return archiveCleaningProcessInformationsControler_;
    //        }
    //        if (pAbstractProcessInformations instanceof ArchivingProcessInformations) {
    //            return archivingProcessInformationsControler_;
    //        }
    //        if (pAbstractProcessInformations instanceof CatalogueUpdateProcessInformations) {
    //            return catalogueUpdateProcessInformationsControler_;
    //        }
    //        if (pAbstractProcessInformations instanceof DeletionProcessInformations) {
    //            return deletionProcessInformationsControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pProcessStatus
    //     * @return
    //     * @since 5.2
    //     */
    //    public static ProcessStatusControler getControler(ProcessStatus pProcessStatus) {
    //        if (pProcessStatus instanceof ProcessStatus) {
    //            return processStatusControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pPlugin
    //     * @return
    //     * @since 5.2
    //     */
    //    public static PluginControler getControler(Plugin pPlugin) {
    //        if (pPlugin instanceof FilePlugin) {
    //            return getControler((FilePlugin) pPlugin);
    //        }
    //        if (pPlugin instanceof ProductPlugin) {
    //            return getControler((ProductPlugin) pPlugin);
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pProductPlugin
    //     * @return
    //     * @since 5.2
    //     */
    //    public static ProductPluginControler getControler(ProductPlugin pProductPlugin) {
    //        if (pProductPlugin instanceof PostAcquisitionPlugin) {
    //            return postAcquisitionPluginControler_;
    //        }
    //        if (pProductPlugin instanceof ProductMetaDataCreationPlugin) {
    //            return productMetaDataCreationPluginControler_;
    //        }
    //        return null;
    //    }
    //
    //    /**
    //     * @param pFilePlugin
    //     * @return
    //     * @since 5.2
    //     */
    //    public static FilePluginControler getControler(FilePlugin pFilePlugin) {
    //        if (pFilePlugin instanceof FileCheckingPlugin) {
    //            return fileCheckingPluginControler_;
    //        }
    //        if (pFilePlugin instanceof FileMetaDataCreationPlugin) {
    //            return fileMetaDataCreationPluginControler_;
    //        }
    //        return null;
    //    }
    //    
    //        public static CompositeAttributeControler getControler(Attribute pAttribute) {
    //            if (pAttribute instanceof CompositeAttribute) {
    //                return compositeAttributeControler_;
    //            }
    //            return null;
    //        }
    //    public static SsaltoFileStatusControler getControler(SsaltoFileStatus pSsaltoFileStatus) {
    //        if (pSsaltoFileStatus instanceof SsaltoFileStatus) {
    //            return ssaltoFileStatusControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static SsaltoFileControler getControler(SsaltoFile pSsaltoFile) {
    //        if (pSsaltoFile instanceof SsaltoFile) {
    //            return ssaltoFileControler_;
    //        }
    //        return null;
    //    }
    //
    //    public static PhysicalFileControler getControler(PhysicalFile pPhysicalFile) {
    //
    //        if (pPhysicalFile instanceof TarPhysicalFile) {
    //            return tarPhysicalFileControler_;
    //        }
    //        if (pPhysicalFile instanceof CutPhysicalFile) {
    //            return cutPhysicalFileControler_;
    //        }
    //        return physicalFileControler_;
    //    }

    /**
    *
    * Récupère le controleur de l'attribut en fonction de son type
    *
    * @param pAttribute
    *            l'attribut
    * @return le contrôleur de l'attribut ou null
    * @since 5.2
    */
    public static AttributeControler getControler(Attribute pAttribute) {
        if (pAttribute instanceof ClobAttribute) {
            return clobAttributeControler;
        }
        if (pAttribute instanceof DateAttribute) {
            return getControler((DateAttribute) pAttribute);
        }
        if (pAttribute instanceof DateTimeAttribute) {
            return getControler((DateTimeAttribute) pAttribute);
        }
        if (pAttribute instanceof GeoAttribute) {
            return geoAttributeControler;
        }
        if (pAttribute instanceof LongAttribute) {
            return longAttributeControler;
        }
        if (pAttribute instanceof RealAttribute) {
            return realAttributeControler;
        }
        if (pAttribute instanceof StringAttribute) {
            return stringAttributeControler;
        }
        if (pAttribute instanceof UrlAttribute) {
            return urlAttributeControler;
        }
        if (pAttribute instanceof CompositeAttribute) {
            return compositeAttributeControler_;
        }
        return null;
    }

    /**
    *
    * Récupère le controleur de l'attribut en fonction de son type
    *
    * @param pAttribute
    *            l'attribut
    * @return le contrôleur de l'attribut ou null
    * @since 5.2
    */
    public static DateAttributeControler getControler(DateAttribute pAttribute) {
        if (pAttribute instanceof DescriptorDateAttribute) {
            return descriptorDateAttributeControler;
        }
        if (pAttribute instanceof DateAttribute) {
            return dateAttributeControler;
        }
        return null;
    }

    /**
    *
    * Récupère le controleur de l'attribut en fonction de son type
    *
    * @param pAttribute
    *            l'attribut
    * @return le contrôleur de l'attribut ou null
    * @since 5.2
    */
    public static DateTimeAttributeControler getControler(DateTimeAttribute pAttribute) {
        if (pAttribute instanceof DescriptorDateTimeAttribute) {
            return descriptorDateTimeAttributeControler;
        }
        if (pAttribute instanceof DateTimeAttribute) {
            return dateTimeAttributeControler;
        }
        return null;
    }
}
