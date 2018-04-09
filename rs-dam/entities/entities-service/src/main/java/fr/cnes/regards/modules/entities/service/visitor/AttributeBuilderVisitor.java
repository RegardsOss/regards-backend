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
package fr.cnes.regards.modules.entities.service.visitor;

import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.builder.AttributeBuilder;
import fr.cnes.regards.modules.models.domain.ComputationPlugin;
import fr.cnes.regards.modules.models.domain.IComputedAttribute;
import fr.cnes.regards.modules.models.domain.IComputedAttributeVisitor;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;

/**
 * Visitor handling the logic of creating an AbstractAttribute according to the AttributeModel computed by the
 * ICalculationModel plugin
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class AttributeBuilderVisitor implements IComputedAttributeVisitor<AbstractAttribute<?>> {

    @Override
    public <P, U> AbstractAttribute<?> visit(IComputedAttribute<P, U> plugin) {
        AttributeModel attr = plugin.getAttributeToCompute();
        ComputationPlugin computationPlugin = plugin.getClass().getAnnotation(ComputationPlugin.class);
        AttributeType attributeType = computationPlugin.supportedType();
        if (attr.getFragment().isDefaultFragment()) {
            return AttributeBuilder.forType(attributeType, attr.getName(), plugin.getResult());
        } else {
            return AttributeBuilder.buildObject(attr.getFragment().getName(),
                                                AttributeBuilder
                                                        .forType(attributeType, attr.getName(), plugin.getResult()));
        }
    }

}
