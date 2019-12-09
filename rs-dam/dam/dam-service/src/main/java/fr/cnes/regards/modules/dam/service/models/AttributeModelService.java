/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service.models;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.dam.dao.entities.ICollectionRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.dao.models.AttributeModelSpecifications;
import fr.cnes.regards.modules.dam.dao.models.IAttributeModelRepository;
import fr.cnes.regards.modules.dam.dao.models.IAttributePropertyRepository;
import fr.cnes.regards.modules.dam.dao.models.IFragmentRepository;
import fr.cnes.regards.modules.dam.dao.models.IModelAttrAssocRepository;
import fr.cnes.regards.modules.dam.dao.models.IRestrictionRepository;
import fr.cnes.regards.modules.dam.domain.models.ModelAttrAssoc;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeModel;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeProperty;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeType;
import fr.cnes.regards.modules.dam.domain.models.attributes.Fragment;
import fr.cnes.regards.modules.dam.domain.models.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.dam.domain.models.attributes.restriction.IRestriction;
import fr.cnes.regards.modules.dam.domain.models.event.AttributeModelCreated;
import fr.cnes.regards.modules.dam.domain.models.event.AttributeModelDeleted;
import fr.cnes.regards.modules.dam.service.models.event.NewFragmentAttributeEvent;
import fr.cnes.regards.modules.dam.service.models.exception.UnsupportedRestrictionException;

/**
 * Manage global attribute life cycle
 * @author msordi
 */
@Service
@MultitenantTransactional
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

    private final IModelAttrAssocRepository modelAttrAssocRepository;

    private final IDatasetRepository datasetRepository;

    private final ICollectionRepository collectionRepository;

    /**
     * Publish for model changes
     */
    private final IPublisher publisher;

    /**
     * Application Event publisher to publish event inside the microservice
     */
    private final ApplicationEventPublisher eventPublisher;

    public AttributeModelService(IAttributeModelRepository pAttModelRepository,
            IRestrictionRepository pRestrictionRepository, IFragmentRepository pFragmentRepository,
            IAttributePropertyRepository pAttPropertyRepository, IModelAttrAssocRepository modelAttrAssocRepository,
            IDatasetRepository datasetRepository, ICollectionRepository collectionRepository,
            IPublisher pPublisher, ApplicationEventPublisher eventPublisher) {
        attModelRepository = pAttModelRepository;
        restrictionRepository = pRestrictionRepository;
        fragmentRepository = pFragmentRepository;
        attPropertyRepository = pAttPropertyRepository;
        this.modelAttrAssocRepository = modelAttrAssocRepository;
        this.datasetRepository = datasetRepository;
        this.collectionRepository = collectionRepository;
        publisher = pPublisher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<AttributeModel> getAttributes(AttributeType type, String fragmentName, Set<Long> modelIds,
            Set<String> modelNames) {
        return attModelRepository
                .findAll(AttributeModelSpecifications.search(type, fragmentName, modelIds, modelNames));
    }

    @Override
    public AttributeModel addAttribute(AttributeModel attributeModel, boolean duringImport) throws ModuleException {
        AttributeModel created = createAttribute(attributeModel);
        // During imports all modelAttrAssoc are created by the importing methods so we have not to publish the event.
        // Otherwise we will try to create duplicates into the DB and break the import
        if (!duringImport && !created.getFragment().isDefaultFragment()) {
            eventPublisher.publishEvent(new NewFragmentAttributeEvent(attributeModel));
        }
        // Publish attribute creation
        publisher.publish(new AttributeModelCreated(attributeModel));
        return attributeModel;
    }

    @Override
    public Iterable<AttributeModel> addAllAttributes(Iterable<AttributeModel> attributeModels) throws ModuleException {
        if (attributeModels != null) {
            for (AttributeModel attModel : attributeModels) {
                addAttribute(attModel, false);
            }
        }
        return attributeModels;
    }

    @Override
    public AttributeModel getAttribute(Long attributeId) throws ModuleException {
        Optional<AttributeModel> attModelOpt = attModelRepository.findById(attributeId);
        if (!attModelOpt.isPresent()) {
            throw new EntityNotFoundException(attributeId, AttributeModel.class);
        }
        return attModelOpt.get();
    }

    @Override
    public AttributeModel updateAttribute(Long id, AttributeModel attributeModel) throws ModuleException {
        if (!attributeModel.isIdentifiable()) {
            throw new EntityNotFoundException(
                    String.format("Unknown identifier for attribute model \"%s\"", attributeModel.getName()));
        }
        if (!id.equals(attributeModel.getId())) {
            throw new EntityInconsistentIdentifierException(id, attributeModel.getId(), attributeModel.getClass());
        }
        if (!attModelRepository.existsById(id)) {
            throw new EntityNotFoundException(attributeModel.getId(), AttributeModel.class);
        }
        manageRestriction(attributeModel);
        return attModelRepository.save(attributeModel);
    }

    @Override
    public void deleteAttribute(Long attributeId) throws ModuleException {
        Optional<AttributeModel> attModOpt = attModelRepository.findById(attributeId);
        if (attModOpt.isPresent()) {
            if (!isDeletable(attributeId)) {
                String errorMessage = "Attribute cannot be deleted because already linked to at least one entity or datasource";
                LOGGER.error(errorMessage);
                throw new EntityOperationForbiddenException(String.valueOf(attributeId), AttributeModel.class,
                        errorMessage);
            }

            attModelRepository.deleteById(attributeId);
            // Publish attribute deletion
            publisher.publish(new AttributeModelDeleted(attModOpt.get()));
        }
    }

    @Override
    public boolean isDeletable(Long attributeId) {
        // Before deletion, look for already linked models
        Collection<ModelAttrAssoc> assocs = modelAttrAssocRepository.findAllByAttributeId(attributeId);
        if (!assocs.isEmpty()) {
            Set<Long> modelIds = new HashSet<>();
            Set<String> modelNames = new HashSet<>();
            assocs.forEach(a -> {
                modelIds.add(a.getModel().getId());
                modelNames.add(a.getModel().getName());
            });
            // Check if linked models not already used, so attribute must not be deleted
            if (datasetRepository.isLinkedToEntities(modelIds) || collectionRepository.isLinkedToEntities(modelIds)
                    || datasetRepository.isLinkedToDatasetsAsDataModel(modelNames)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AttributeModel createAttribute(AttributeModel attributeModel) throws ModuleException {
        manageRestriction(attributeModel);
        manageFragment(attributeModel);
        manageProperties(attributeModel);
        manageAttributeModel(attributeModel);
        return attributeModel;
    }

    /**
     * Manage attribute model restriction
     * @param attributeModel attribute model
     * @throws UnsupportedRestrictionException if restriction not supported
     */
    private void manageRestriction(AttributeModel attributeModel) throws UnsupportedRestrictionException {
        final AbstractRestriction restriction = attributeModel.getRestriction();
        if (restriction != null) {
            checkRestrictionSupport(attributeModel);
            restrictionRepository.save(restriction);
        }
    }

    /**
     * Manage attribute model fragment (fallback to default fragment)
     * @param attributeModel attribute model
     * @return fragment
     */
    private Fragment manageFragment(AttributeModel attributeModel) throws EntityAlreadyExistsException {
        Fragment fragment = attributeModel.getFragment();
        if (fragment != null) {
            fragment = initOrRetrieveFragment(fragment);
        } else {
            // Fallback to default fragment
            fragment = initOrRetrieveFragment(Fragment.buildDefault());
        }
        attributeModel.setFragment(fragment);
        return fragment;
    }

    private void manageProperties(AttributeModel attributeModel) {
        if (attributeModel.getProperties() != null) {
            attPropertyRepository.saveAll(attributeModel.getProperties());
        }
    }

    private Fragment initOrRetrieveFragment(Fragment inFragment) throws EntityAlreadyExistsException {
        Fragment fragment = fragmentRepository.findByName(inFragment.getName());
        if (fragment == null) {
            inFragment.setId(null);
            if (!isFragmentCreatable(inFragment.getName())) {
                throw new EntityAlreadyExistsException(String
                        .format("Fragment with name \"%s\" cannot be created because an attribute with the same name already exists!",
                                inFragment.getName()));
            }
            fragment = fragmentRepository.save(inFragment);
        }
        return fragment;
    }

    /**
     * Manage a single attribute model
     * @param inAttributeModel the attribute model
     * @return the persisted attribute model
     * @throws ModuleException if conflict detected
     */
    private AttributeModel manageAttributeModel(AttributeModel inAttributeModel) throws ModuleException {
        if (!inAttributeModel.isIdentifiable()) {
            // Check potential conflict
            AttributeModel attributeModel = attModelRepository
                    .findByNameAndFragmentName(inAttributeModel.getName(), inAttributeModel.getFragment().getName());
            if (attributeModel != null) {
                String message;
                if (inAttributeModel.getFragment().isDefaultFragment()) {
                    message = MessageFormat.format("Attribute model with name \"{0}\" already exists.",
                                                   inAttributeModel.getName());
                } else {
                    message = MessageFormat
                            .format("Attribute model with name \"{0}\" in fragment \"{1}\" already exists.",
                                    inAttributeModel.getName(), inAttributeModel.getFragment().getName());
                }
                LOGGER.error(message);
                throw new EntityAlreadyExistsException(message);
            }
            if (fragmentRepository.findByName(inAttributeModel.getName()) != null) {
                throw new EntityAlreadyExistsException(MessageFormat
                        .format("Attribute with name \"{0}\" cannot be created because a fragment with the same name already exists",
                                inAttributeModel.getName()));
            }
        }
        return attModelRepository.save(inAttributeModel);
    }

    @Override
    public boolean isFragmentAttribute(Long attributeId) throws ModuleException {
        Optional<AttributeModel> attModelOpt = attModelRepository.findById(attributeId);
        if (!attModelOpt.isPresent()) {
            throw new EntityNotFoundException(attributeId, AttributeModel.class);
        }
        return !attModelOpt.get().getFragment().isDefaultFragment();
    }

    @Override
    public List<AttributeModel> findByFragmentId(Long pFragmentId) {
        return attModelRepository.findByFragmentId(pFragmentId);
    }

    @Override
    public List<AttributeModel> findByFragmentName(String pFragmentName) {
        return attModelRepository.findByFragmentName(pFragmentName);
    }

    @Override
    public void checkRestrictionSupport(AttributeModel pAttributeModel) throws UnsupportedRestrictionException {
        IRestriction restriction = pAttributeModel.getRestriction();
        if ((restriction != null) && !restriction.supports(pAttributeModel.getType())) {
            String message = String.format("Attribute of type %s does not support %s restriction",
                                           pAttributeModel.getType(), restriction.getType());
            LOGGER.error(message);
            throw new UnsupportedRestrictionException(message);
        }
    }

    @Override
    public AttributeModel findByNameAndFragmentName(String pAttributeName, String pFragmentName) {
        if (Strings.isNullOrEmpty(pFragmentName)) {
            return attModelRepository.findByNameAndFragmentName(pAttributeName, Fragment.getDefaultName());
        }
        return attModelRepository.findByNameAndFragmentName(pAttributeName, pFragmentName);
    }

    @Override
    public boolean isFragmentCreatable(String fragmentName) {
        return attModelRepository.findByName(fragmentName).isEmpty();
    }

}
