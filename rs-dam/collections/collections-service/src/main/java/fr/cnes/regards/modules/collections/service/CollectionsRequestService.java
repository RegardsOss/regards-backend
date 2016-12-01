/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.collections.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.modules.collections.dao.ICollectionRepository;
import fr.cnes.regards.modules.collections.domain.Collection;
import fr.cnes.regards.modules.entities.dao.IAbstractEntityRepository;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Tag;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.service.IStorageService;

/**
 * @author lmieulet
 * @author Sylvain Vissiere-Guerinet
 */
@Service
public class CollectionsRequestService implements ICollectionsRequestService {

    // TODO: interactions with catalog
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CollectionsRequestService.class);

    /**
     * bean toward the module responsible to contact, or not, the archival storage
     */
    private final IStorageService storageService;

    /**
     * DAO autowired by Spring
     */
    private final ICollectionRepository collectionRepository;

    /**
     * DAO for all entities, used to dissociate
     */
    private final IAbstractEntityRepository<AbstractEntity> entitiesRepository;

    /**
     *
     * @param pCollectionRepository
     *            repository used by service to provide data
     * @param pPersistService
     *            service used to contact, or not, archival storage
     */
    public CollectionsRequestService(ICollectionRepository pCollectionRepository,
            IAbstractEntityRepository<AbstractEntity> pAbstractEntityRepository, IStorageService pPersistService) {
        super();
        collectionRepository = pCollectionRepository;
        storageService = pPersistService;
        entitiesRepository = pAbstractEntityRepository;
    }

    @Override
    public Collection retrieveCollectionById(Long pCollectionId) {
        return collectionRepository.findOne(pCollectionId);
    }

    @Override
    public Collection updateCollection(Collection pCollection, Long pCollectionId)
            throws EntityInconsistentIdentifierException {
        // Check if exist
        final Collection collectionBeforeUpdate = collectionRepository.findOne(pCollectionId);
        if (!collectionBeforeUpdate.getId().equals(pCollection.getId())) {
            // "collectionId inside the route is different from the entity inside the request body"
            throw new EntityInconsistentIdentifierException(pCollectionId, pCollection.getId(), Collection.class);
        }
        // FIXME: handle the URN and possible modifications (version change/Revisions)
        Set<UniformResourceName> oldLinks = extractUrns(collectionBeforeUpdate.getTags());
        Set<UniformResourceName> newLinks = extractUrns(pCollection.getTags());
        if (!oldLinks.equals(newLinks)) {
            // TODO: associate/dissociate cf REGARDS_DSL_DAM_COL_220
            final Set<UniformResourceName> toDissociate = getDiff(oldLinks, newLinks);
            dissociate(collectionBeforeUpdate, toDissociate);
            final Set<UniformResourceName> toAssociate = getDiff(newLinks, oldLinks);
            associate(collectionBeforeUpdate, toAssociate);
        }
        Collection updatedCollection = collectionRepository.save(pCollection);
        storageService.persist(updatedCollection);
        return updatedCollection;
    }

    /**
     * @param pCollection
     *            a {@link Collection}
     * @param pToAssociate
     *            {@link Set} of {@link UniformResourceName}s representing {@link AbstractEntity} to associate to
     *            pCollection
     */
    private void associate(Collection pCollection, Set<UniformResourceName> pToAssociate) {
        final List<AbstractEntity> entityToAssociate = entitiesRepository.findByIpIdIn(pToAssociate);
        associate(pCollection, entityToAssociate);
    }

    /**
     * @param pCollection
     *            a {@link Collection}
     * @param pToDissociate
     *            {@link Set} of {@link UniformResourceName}s representing {@link AbstractEntity} to dissociate from
     *            pCollection
     */
    private void dissociate(Collection pCollection, Set<UniformResourceName> pToDissociate) {
        final List<AbstractEntity> entityToDissociate = entitiesRepository.findByIpIdIn(pToDissociate);
        dissociate(pCollection, entityToDissociate);
    }

    /**
     * @param pSource
     *            {@link Set} of {@link UniformResourceName}
     * @param pOther
     *            {@link Set} of {@link UniformResourceName} to remove from pSource
     * @return a new {@link Set} of {@link UniformResourceName} containing only the elements present into pSource and
     *         not in pOther
     */
    private Set<UniformResourceName> getDiff(Set<UniformResourceName> pSource, Set<UniformResourceName> pOther) {
        final Set<UniformResourceName> result = new HashSet<>();
        result.addAll(pSource);
        result.removeAll(pOther);
        return result;
    }

    /**
     * @param pTags
     * @return
     */
    private Set<UniformResourceName> extractUrns(Set<Tag> pTags) {
        return pTags.parallelStream().filter(t -> UniformResourceName.isValidUrn(t.getValue()))
                .map(t -> UniformResourceName.fromString(t.getValue())).collect(Collectors.toSet());
    }

    @Override
    public void deleteCollection(Long pCollectionId) {
        final Collection toDelete = collectionRepository.findOne(pCollectionId);
        dissociate(toDelete);
        // FIXME: repo.delete and then persist.delete? ou c'est que storage qui gère le delete?
        collectionRepository.delete(pCollectionId);
        storageService.delete(toDelete);
    }

    private void dissociate(Collection pToDelete) {
        final List<AbstractEntity> linkedToToDelete = entitiesRepository
                .findByTagsValue(pToDelete.getIpId().toString());
        dissociate(pToDelete, linkedToToDelete);
    }

    private void dissociate(Collection pToDissociate, List<AbstractEntity> pToBeDissociated) {
        // TODO: implement cf REGARDS_DSL_DAM_COL_120
        final Set<Tag> toDissociateAssociations = pToDissociate.getTags();
        for (AbstractEntity toBeDissociated : pToBeDissociated) {
            toDissociateAssociations.remove(new Tag(toBeDissociated.getIpId().toString()));
            dissociate(pToDissociate, toBeDissociated);
        }
        pToDissociate.setTags(toDissociateAssociations);
        collectionRepository.save(pToDissociate);
    }

    /**
     * dissociate actual from target
     *
     * @param pActual
     * @param pTarget
     */
    private void dissociate(Collection pActual, AbstractEntity pTarget) {
        // TODO: REGARDS_DSL_DAM_COL_230
        pTarget.getTags().remove(new Tag(pActual.getIpId().toString()));
        entitiesRepository.save(pTarget);
    }

    /**
     * Associate source and target
     *
     * @param pSource
     * @param pCollectionList
     */
    public void associate(Collection pSource, List<AbstractEntity> pCollectionList) {
        // TODO: implement cf REGARDS_DSL_DAM_COL_040
        for (AbstractEntity target : pCollectionList) {
            // associate target to source
            pSource.getTags().add(new Tag(target.getIpId().toString()));
            associate(pSource, target);
        }
        collectionRepository.save(pSource);
    }

    /**
     * Associate source to target
     *
     * @param pSource
     * @param pTarget
     */
    private void associate(Collection pSource, AbstractEntity pTarget) {
        // TODO: implement cf REGARDS_DSL_DAM_COL_050
        pTarget.getTags().add(new Tag(pSource.getIpId().toString()));
        entitiesRepository.save(pTarget);
    }

    @Override
    public Collection createCollection(Collection pCollection) {
        // TODO: verify with model
        Collection newCollection = collectionRepository.save(pCollection);
        if (!newCollection.getTags().isEmpty()) {
            associate(newCollection);
        }
        newCollection = storageService.persist(newCollection);
        return newCollection;
    }

    /**
     * @param pNewCollection
     */
    private void associate(Collection pNewCollection) {
        final Set<Tag> tags = pNewCollection.getTags();
        final Set<UniformResourceName> toAssociateIpIds = extractUrns(tags);
        final List<AbstractEntity> toLink = entitiesRepository.findByIpIdIn(toAssociateIpIds);
        associate(pNewCollection, toLink);
    }

    @Override
    public List<Collection> retrieveCollectionList() {
        return collectionRepository.findAll();
    }

    @Override
    public Collection retrieveCollectionByIpId(String pCollectionIpId) {
        return collectionRepository.findOneByIpId(pCollectionIpId);
    }

    @Override
    public void deleteCollection(String pCollectionIpId) {
        final Collection toDelete = collectionRepository.findOneByIpId(pCollectionIpId);
        collectionRepository.deleteByIpId(pCollectionIpId);
        storageService.delete(toDelete);

    }

    @Override
    public Collection dissociateCollection(Long pCollectionId, List<AbstractEntity> pToBeDissociated) {
        final Collection dissociatedCollection = collectionRepository.findOne(pCollectionId);
        dissociate(dissociatedCollection, pToBeDissociated);
        return dissociatedCollection;
    }

    @Override
    public Collection associateCollection(Long pCollectionId, List<AbstractEntity> pToBeAssociatedWith) {
        final Collection associatedCollection = collectionRepository.findOne(pCollectionId);
        associate(associatedCollection, pToBeAssociatedWith);
        return associatedCollection;
    }

}
