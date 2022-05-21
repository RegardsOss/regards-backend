package fr.cnes.regards.modules.indexer.dao;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.indexer.domain.criterion.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class VersioningSearchVisitor implements ICriterionVisitor<ICriterion> {

    private final IEsRepository esRepo;

    public VersioningSearchVisitor(IEsRepository esRepo) {
        this.esRepo = esRepo;
    }

    @Override
    public ICriterion visitEmptyCriterion(EmptyCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitAndCriterion(AbstractMultiCriterion criterion) {
        List<ICriterion> newCriteria = new ArrayList<>();
        for (ICriterion existingCriterion : criterion.getCriterions()) {
            newCriteria.add(existingCriterion.accept(this));
        }
        return ICriterion.and(newCriteria);
    }

    @Override
    public ICriterion visitOrCriterion(AbstractMultiCriterion criterion) {
        List<ICriterion> newCriteria = new ArrayList<>();
        for (ICriterion existingCriterion : criterion.getCriterions()) {
            newCriteria.add(existingCriterion.accept(this));
        }
        return ICriterion.or(newCriteria);
    }

    @Override
    public ICriterion visitNotCriterion(NotCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitStringMatchCriterion(StringMatchCriterion criterion) {
        return handleVersioning(criterion);
    }

    @Override
    public ICriterion visitStringMultiMatchCriterion(StringMultiMatchCriterion criterion) {
        return handleVersioning(criterion);
    }

    @Override
    public ICriterion visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion) {
        return handleVersioning(criterion);
    }

    private ICriterion handleVersioning(StringMatchCriterion criterion) {
        // we only need to handle criterion on tags because otherwise, we ask for something precise
        // if the value is a virtualId, we have to find the entity and add ipId
        // if the value is an ipId, we need to find the entity and add the virtualId if any
        // anyway it is a urn so we can recognize if anything is needed thanks to URN pattern
        String attName = criterion.getName();
        String critValue = criterion.getValue();
        if (attName.matches(StaticProperties.FEATURE_TAGS) && critValue.matches(UniformResourceName.URN_PATTERN)) {
            UniformResourceName urn = UniformResourceName.fromString(critValue);
            if (urn.isLast()) {
                // this is a virtualId
                // lets find the corresponding entity
                AbstractEntity entity = esRepo.getByVirtualId(urn.getEntityType().toString(),
                                                              critValue,
                                                              AbstractEntity.class);
                if (entity != null) {
                    return ICriterion.or(ICriterion.eq(attName, critValue, criterion.getMatchType()),
                                         ICriterion.eq(attName, entity.getIpId().toString(), criterion.getMatchType()));
                } else {
                    // we are looking for something that does not exists(anymore?) so we don't need to change the criterion
                    return criterion;
                }
            } else {
                // this is a precise urn
                AbstractEntity entity = esRepo.get(urn.getEntityType().toString(), critValue, AbstractEntity.class);
                if (entity != null && entity.isLast()) {
                    return ICriterion.or(ICriterion.eq(attName,
                                                       entity.getVirtualId().toString(),
                                                       criterion.getMatchType()),
                                         ICriterion.eq(attName, critValue, criterion.getMatchType()));
                } else {
                    // we are looking for something that does not exists(anymore?) so we don't need to change the criterion
                    return criterion;
                }
            }
        } else {
            return criterion;
        }
    }

    private ICriterion handleVersioning(StringMultiMatchCriterion criterion) {
        // we only need to handle criterion on tags because otherwise, we ask for something precise
        // if the value is a virtualId, we have to find the entity and add ipId
        // if the value is an ipId, we need to find the entity and add the virtualId if any
        // anyway it is a urn so we can recognize if anything is needed thanks to URN pattern
        Set<String> attNames = criterion.getNames();
        String critValue = criterion.getValue();
        if (attNames.contains(StaticProperties.FEATURE_TAGS) && critValue.matches(UniformResourceName.URN_PATTERN)) {
            UniformResourceName urn = UniformResourceName.fromString(critValue);
            if (urn.isLast()) {
                // this is a virtualId
                // lets find the corresponding entity
                AbstractEntity entity = esRepo.getByVirtualId(urn.getEntityType().toString(),
                                                              critValue,
                                                              AbstractEntity.class);
                return ICriterion.or(ICriterion.multiMatch(attNames, critValue),
                                     ICriterion.multiMatch(attNames, entity.getIpId().toString()));
            } else {
                // this is a precise urn
                AbstractEntity entity = esRepo.get(urn.getEntityType().toString(), critValue, AbstractEntity.class);
                if (entity.isLast()) {
                    return ICriterion.or(ICriterion.multiMatch(attNames, entity.getVirtualId().toString()),
                                         ICriterion.multiMatch(attNames, critValue));
                } else {
                    return criterion;
                }
            }
        } else {
            return criterion;
        }
    }

    private ICriterion handleVersioning(StringMatchAnyCriterion criterion) {
        // we only need to handle criterion on tags because otherwise, we ask for something precise
        String attName = criterion.getName();
        List<String> critValues = Arrays.asList(criterion.getValue());
        if (attName.matches(StaticProperties.FEATURE_TAGS)) {
            // if the values contains virtualIds, we have to find the entities and add their ipId
            // if the value contains ipIds, we need to find the entities and add their virtualId if any
            // anyway it is a urn so we can recognize if anything is needed thanks to URN pattern
            Set<String> urnValues = critValues.stream()
                                              .filter(value -> value.matches(UniformResourceName.URN_PATTERN))
                                              .collect(Collectors.toSet());
            if (urnValues.isEmpty()) {
                return criterion;
            } else {
                for (String urnValue : urnValues) {
                    UniformResourceName urn = UniformResourceName.fromString(urnValue);
                    if (urn.isLast()) {
                        // this is a virtualId
                        // lets find the corresponding entity
                        AbstractEntity entity = esRepo.getByVirtualId(urn.getEntityType().toString(),
                                                                      urnValue,
                                                                      AbstractEntity.class);
                        critValues.add(entity.getIpId().toString());
                    } else {
                        // this is a precise urn
                        AbstractEntity entity = esRepo.get(urn.getEntityType().toString(),
                                                           urnValue,
                                                           AbstractEntity.class);
                        if (entity.isLast()) {
                            urnValues.add(entity.getVirtualId().toString());
                        }
                    }
                }
                // now that we have added values needed, just recreate the criterion
                return ICriterion.in(attName, criterion.getMatchType(), critValues.toArray(new String[0]));
            }
        } else {
            return criterion;
        }
    }

    @Override
    public ICriterion visitIntMatchCriterion(IntMatchCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitLongMatchCriterion(LongMatchCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitDateMatchCriterion(DateMatchCriterion criterion) {
        return criterion;
    }

    @Override
    public <U extends Comparable<? super U>> ICriterion visitRangeCriterion(RangeCriterion<U> criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitDateRangeCriterion(DateRangeCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitBooleanMatchCriterion(BooleanMatchCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitPolygonCriterion(PolygonCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitCircleCriterion(CircleCriterion criterion) {
        return criterion;
    }

    @Override
    public ICriterion visitFieldExistsCriterion(FieldExistsCriterion criterion) {
        return criterion;
    }
}
