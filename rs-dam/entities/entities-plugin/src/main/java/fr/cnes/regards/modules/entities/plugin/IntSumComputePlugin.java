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
package fr.cnes.regards.modules.entities.plugin;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.IntegerAttribute;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.models.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;

/**
 * This Implementation of IComputedAttribute allows to compute the sum of {@link IntegerAttribute} according to a
 * collection of {@link DataObject} using the same IntegerAttribute name
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(id = "IntSumComputePlugin", version = "1.0.0",
        description = "allows to compute the sum of IntegerAttribute according to a collection of data using the same IntegerAttribute name",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class IntSumComputePlugin extends AbstractDataObjectComputePlugin<Integer> {

    @Autowired
    private IEsRepository esRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAttributeModelRepository attModelRepos;

    @PluginParameter(name = RESULT_ATTRIBUTE_NAME, label = "Result attribute name",
            description = "Name of attribute to compute (ie result attribute).")
    private String attributeToComputeName;

    @PluginParameter(name = RESULT_FRAGMENT_NAME, label = "Result fragment name",
            description = "Name of attribute to compute fragment. If computed attribute belongs to default fragment, this value can be set to null.",
            optional = true)
    private String attributeToComputeFragmentName;

    @PluginParameter(name = PARAMETER_ATTRIBUTE_NAME, label = "Parameter attribute name",
            description = "Name of parameter attribute used to compute result attribute.")
    private String parameterAttributeName;

    @PluginParameter(name = PARAMETER_FRAGMENT_NAME, label = "Parameter fragment name",
            description = "Name of parameter attribute fragment. If parameter attribute belongs to default fragment, this value can be set to null.",
            optional = true)
    private String parameterAttributeFragmentName;

    /**
     * Plugin initialization method
     */
    @PluginInit
    public void init() {
        super.initAbstract(esRepo, attModelRepos, tenantResolver);
        super.init(attributeToComputeName, attributeToComputeFragmentName, parameterAttributeName,
                   parameterAttributeFragmentName);
        super.result = 0;
    }

    private void doSum(Optional<AbstractAttribute<?>> propertyOpt) {
        if (propertyOpt.isPresent()) {
            Integer value = ((Number) propertyOpt.get().getValue()).intValue();
            if (value != null) {
                super.result += value;
            }
        }
    }

    @Override
    public AttributeType getSupported() {
        return AttributeType.INTEGER;
    }

    /**
     * @param dataset dataset on which the attribute, once computed, will be added. This allows us to know which
     * DataObject should be used.
     */
    @Override
    public void compute(Dataset dataset) {
        result = null;
        // create the search
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(tenantResolver.getTenant(),
                                                                      EntityType.DATA.toString(), DataObject.class);
        Double doubleResult = esRepo.sum(searchKey, dataset.getSubsettingClause(), parameterAttribute.getJsonPath());
        result = doubleResult.intValue();
        LOG.debug("Attribute {} computed for Dataset {}. Result: {}", parameterAttribute.getJsonPath(),
                  dataset.getIpId().toString(), result);
    }

    @Override
    protected Consumer<DataObject> doCompute() {
        super.result = 0;
        return object -> doSum(extractProperty(object));
    }

}
