/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    /*
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

    /*
     * Attribute controlers
     */
    private static CompositeAttributeControler compositeAttributeControler_ = new CompositeAttributeControler();

    public static DataObjectElementControler getControler(DataObjectElement pDataObjectElement) {
        if (pDataObjectElement instanceof DataObjectDescriptionElement) {
            return dataObjectDescriptionElementControler;
        }
        if (pDataObjectElement instanceof DataObjectUpdateElement) {
            return dataObjectUpdateElementControler;
        }
        return null;
    }

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
