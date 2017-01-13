/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.models.service;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotIdentifiableException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.models.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.models.dao.IAttributePropertyRepository;
import fr.cnes.regards.modules.models.dao.IFragmentRepository;
import fr.cnes.regards.modules.models.dao.IRestrictionRepository;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeProperty;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.models.domain.attributes.Fragment;
import fr.cnes.regards.modules.models.domain.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.IRestriction;
import fr.cnes.regards.modules.models.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.models.domain.event.AttributeModelDeleted;
import fr.cnes.regards.modules.models.service.exception.UnsupportedRestrictionException;

/**
 *
 * Manage global attribute life cycle
 *
 * @author msordi
 *
 */
@Service
public class AttributeModelService implements IAttributeModelService {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeModelService.class);

    /**
     * {@link AttributeModel} repository
     */
    private final IAttributeModelRepository attModelRepository;

    /**
     * {@link IRestriction} repository
     */
    private final IRestrictionRepository restrictionRepository;

    /**
     * {@link Fragment} repository
     */
    private final IFragmentRepository fragmentRepository;

    /**
     * {@link AttributeProperty} repository
     */
    private final IAttributePropertyRepository attPropertyRepository;

    /**
     * Publish for model changes
     */
    private final IPublisher publisher;

    public AttributeModelService(IAttributeModelRepository pAttModelRepository,
            IRestrictionRepository pRestrictionRepository, IFragmentRepository pFragmentRepository,
            IAttributePropertyRepository pAttPropertyRepository, IPublisher pPublisher) {
        this.attModelRepository = pAttModelRepository;
        this.restrictionRepository = pRestrictionRepository;
        this.fragmentRepository = pFragmentRepository;
        this.attPropertyRepository = pAttPropertyRepository;
        this.publisher = pPublisher;
    }

    @Override
    public List<AttributeModel> getAttributes(AttributeType pType, String pFragmentName) {
        final Iterable<AttributeModel> attModels;
        if (pFragmentName != null) {
            if (pType != null) {
                attModels = attModelRepository.findByTypeAndFragmentName(pType, pFragmentName);
            } else {
                attModels = attModelRepository.findByFragmentName(pFragmentName);
            }
        } else {
            if (pType != null) {
                attModels = attModelRepository.findByType(pType);
            } else {
                attModels = attModelRepository.findAll();
            }
        }
        return (attModels != null) ? Lists.newArrayList(attModels) : Collections.emptyList();
    }

    @MultitenantTransactional
    @Override
    public AttributeModel addAttribute(AttributeModel pAttributeModel) throws ModuleException {
        createAttribute(pAttributeModel);
        // if (!fragment.isDefaultFragment()) {
        // // TODO modelAttributeService.updateNSBind(fragment.getId());
        // // Attention au référence cyclique entre service
        // }
        // Publish attribute creation
        publisher.publish(new AttributeModelCreated(pAttributeModel));
        return pAttributeModel;
    }

    @MultitenantTransactional
    @Override
    public Iterable<AttributeModel> addAllAttributes(Iterable<AttributeModel> pAttributeModels) throws ModuleException {
        if (pAttributeModels != null) {
            for (AttributeModel attModel : pAttributeModels) {
                createAttribute(attModel);
            }
        }
        return pAttributeModels;
    }

    @Override
    public AttributeModel getAttribute(Long pAttributeId) throws ModuleException {
        if (!attModelRepository.exists(pAttributeId)) {
            throw new EntityNotFoundException(pAttributeId, AttributeModel.class);
        }
        return attModelRepository.findOne(pAttributeId);
    }

    @Override
    public AttributeModel updateAttribute(Long pAttributeId, AttributeModel pAttributeModel) throws ModuleException {
        if (!pAttributeModel.isIdentifiable()) {
            throw new EntityNotIdentifiableException(
                    String.format("Unknown identifier for attribute model \"%s\"", pAttributeModel.getName()));
        }
        if (!pAttributeId.equals(pAttributeModel.getId())) {
            throw new EntityInconsistentIdentifierException(pAttributeId, pAttributeModel.getId(),
                    pAttributeModel.getClass());
        }
        if (!attModelRepository.exists(pAttributeId)) {
            throw new EntityNotFoundException(pAttributeModel.getId(), AttributeModel.class);
        }
        manageRestriction(pAttributeModel);
        return attModelRepository.save(pAttributeModel);
    }

    @Override
    public void deleteAttribute(Long pAttributeId) {
        AttributeModel attMod = attModelRepository.findOne(pAttributeId);
        if (attMod != null) {
            attModelRepository.delete(pAttributeId);
            // Publish attribute deletion
            publisher.publish(new AttributeModelDeleted(attMod));
        }
    }

    @Override
    public AttributeModel createAttribute(AttributeModel pAttributeModel) throws ModuleException {
        manageRestriction(pAttributeModel);
        manageFragment(pAttributeModel);
        manageProperties(pAttributeModel);
        manageAttributeModel(pAttributeModel);
        return pAttributeModel;
    }

    /**
     * Manage attribute model restriction
     *
     * @param pAttributeModel
     *            attribute model
     * @throws UnsupportedRestrictionException
     *             if restriction not supported
     */
    private void manageRestriction(AttributeModel pAttributeModel) throws UnsupportedRestrictionException {
        final AbstractRestriction restriction = pAttributeModel.getRestriction();
        if (restriction != null) {
            checkRestrictionSupport(pAttributeModel);
            restrictionRepository.save(restriction);
        }
    }

    /**
     * Manage attribute model fragment (fallback to default fragment)
     *
     * @param pAttributeModel
     *            attribute model
     * @return fragment
     */
    private Fragment manageFragment(AttributeModel pAttributeModel) {
        Fragment fragment = pAttributeModel.getFragment();
        if (fragment != null) {
            fragment = initOrRetrieveFragment(fragment);
        } else {
            // Fallback to default fragment
            fragment = initOrRetrieveFragment(Fragment.buildDefault());
        }
        pAttributeModel.setFragment(fragment);
        return fragment;
    }

    private void manageProperties(AttributeModel pAttributeModel) {
        if (pAttributeModel.getProperties() != null) {
            attPropertyRepository.save(pAttributeModel.getProperties());
        }
    }

    private Fragment initOrRetrieveFragment(Fragment pFragment) {
        Fragment fragment = fragmentRepository.findByName(pFragment.getName());
        if (fragment == null) {
            pFragment.setId(null);
            fragment = fragmentRepository.save(pFragment);
        }
        return fragment;
    }

    /**
     * Manage a single attribute model
     *
     * @param pAttributeModel
     *            the attribute model
     * @return the persisted attribute model
     * @throws ModuleException
     *             if conflict detected
     */
    private AttributeModel manageAttributeModel(AttributeModel pAttributeModel) throws ModuleException {
        if (!pAttributeModel.isIdentifiable()) {
            // Check potential conflict
            final AttributeModel attributeModel = attModelRepository
                    .findByNameAndFragmentName(pAttributeModel.getName(), pAttributeModel.getFragment().getName());
            if (attributeModel != null) {
                final String message;
                if (pAttributeModel.getFragment().isDefaultFragment()) {
                    message = MessageFormat.format("Attribute model with name \"{0}\" already exists.",
                                                   pAttributeModel.getName());
                } else {
                    // CHECKSTYLE:OFF
                    message = MessageFormat.format(
                                                   "Attribute model with name \"{0}\" in fragment \"{1}\" already exists.",
                                                   pAttributeModel.getName(), pAttributeModel.getFragment().getName());
                    // CHECKSTYLE:ON
                }
                LOGGER.error(message);
                throw new EntityAlreadyExistsException(message);
            }
        }
        return attModelRepository.save(pAttributeModel);
    }

    @Override
    public boolean isFragmentAttribute(Long pAttributeId) throws ModuleException {
        final AttributeModel attModel = attModelRepository.findOne(pAttributeId);
        if (attModel == null) {
            throw new EntityNotFoundException(pAttributeId, AttributeModel.class);
        }
        return !attModel.getFragment().isDefaultFragment();
    }

    @Override
    public List<AttributeModel> findByFragmentId(Long pFragmentId) throws ModuleException {
        Iterable<AttributeModel> attributeModels = attModelRepository.findByFragmentId(pFragmentId);
        return (attributeModels != null) ? ImmutableList.copyOf(attributeModels) : Collections.emptyList();
    }

    @Override
    public void checkRestrictionSupport(AttributeModel pAttributeModel) throws UnsupportedRestrictionException {
        final IRestriction restriction = pAttributeModel.getRestriction();
        if ((restriction != null) && !restriction.supports(pAttributeModel.getType())) {
            final String message = String.format("Attribute of type %s does not support %s restriction",
                                                 pAttributeModel.getType(), restriction.getType());
            LOGGER.error(message);
            throw new UnsupportedRestrictionException(message);
        }
    }
}
