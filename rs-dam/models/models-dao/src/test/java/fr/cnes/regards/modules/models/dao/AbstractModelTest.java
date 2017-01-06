/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.models.dao;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Iterables;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;
import fr.cnes.regards.modules.models.domain.attributes.restriction.AbstractRestriction;

/**
 * Common attribute model test methods
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractModelTest extends AbstractDaoTransactionalTest {

    /**
     * Attribute model repository
     */
    @Autowired
    protected IAttributeModelRepository attModelRepository;

    /**
     * Restriction repository
     */
    @Autowired
    protected IRestrictionRepository restrictionRepository;

    /**
     * Fragment repository
     */
    @Autowired
    protected IFragmentRepository fragmentRepository;

    /**
     * Model repository
     */
    @Autowired
    protected IModelRepository modelRepository;

    /**
     * Model attribute repository
     */
    @Autowired
    protected IModelAttributeRepository modelAttributeRepository;

    /**
     *
     * Save an attribute model
     *
     * @param pAttributeModel
     *            entity to save
     * @return the saved attribute model
     */
    protected AttributeModel saveAttribute(final AttributeModel pAttributeModel) {
        Assert.assertNotNull(pAttributeModel);
        // Save restriction if any
        if (pAttributeModel.getRestriction() != null) {
            final AbstractRestriction restriction = pAttributeModel.getRestriction();
            restrictionRepository.save(restriction);
        }
        // Save fragment if any
        if (pAttributeModel.getFragment() != null) {
            final Fragment fragment = pAttributeModel.getFragment();
            fragmentRepository.save(fragment);
        } else {
            Fragment defaultF = fragmentRepository.findByName(Fragment.getDefaultName());
            if (defaultF == null) {
                defaultF = fragmentRepository.save(Fragment.buildDefault());
            }
            pAttributeModel.setFragment(defaultF);
        }
        // Save attribute model
        return attModelRepository.save(pAttributeModel);
    }

    protected AttributeModel findSingle() {
        final Iterable<AttributeModel> atts = attModelRepository.findAll();
        if (Iterables.size(atts) != 1) {
            Assert.fail("Only single result is expected!");
        }
        return Iterables.get(atts, 0);
    }

    /**
     * Create a model
     *
     * @param pName
     *            model name
     * @param pDescription
     *            description
     * @param pModelType
     *            model type
     * @return a model
     */
    protected Model createModel(String pName, String pDescription, EntityType pModelType) {

        final Model model = new Model();
        model.setName(pName);
        model.setType(pModelType);
        model.setDescription(pDescription);

        modelRepository.save(model);
        Assert.assertTrue(model.isIdentifiable());

        final Model retrieved = modelRepository.findOne(model.getId());
        Assert.assertEquals(pName, retrieved.getName());
        Assert.assertEquals(pDescription, retrieved.getDescription());
        Assert.assertEquals(pModelType, retrieved.getType());
        return retrieved;
    }
}
