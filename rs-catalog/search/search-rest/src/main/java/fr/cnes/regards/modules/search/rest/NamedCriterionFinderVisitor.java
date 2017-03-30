/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.Iterator;
import java.util.Optional;

import fr.cnes.regards.modules.indexer.domain.IMapping;
import fr.cnes.regards.modules.indexer.domain.criterion.AbstractMultiCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.CircleCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.DateRangeCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.EmptyCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterionVisitor;
import fr.cnes.regards.modules.indexer.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.LongMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.NotCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchAnyCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;

/**
 * Criterion visitor which tries to find a ICriterion concerning the sepcified attribute name.<br>
 * Returns the {@link Optional} criterion if it found one somewhere in the hierarchy, else optional of null.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NamedCriterionFinderVisitor implements ICriterionVisitor<Optional<ICriterion>> {

    /**
     * The name of the criterion we are looking for
     */
    private final String searchedName;

    /**
     * @param pSearchedName the name of the searched criterion
     */
    public NamedCriterionFinderVisitor(String pSearchedName) {
        super();
        searchedName = pSearchedName;
    }

    @Override
    public Optional<ICriterion> visitEmptyCriterion(EmptyCriterion pCriterion) {
        return Optional.empty();
    }

    @Override
    public Optional<ICriterion> visitAndCriterion(AbstractMultiCriterion pCriterion) {
        Optional<ICriterion> result = Optional.empty();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (!result.isPresent() && criterionIterator.hasNext()) {
            result = criterionIterator.next().accept(this);
        }
        return result;
    }

    @Override
    public Optional<ICriterion> visitOrCriterion(AbstractMultiCriterion pCriterion) {
        Optional<ICriterion> result = Optional.empty();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (!result.isPresent() && criterionIterator.hasNext()) {
            result = criterionIterator.next().accept(this);
        }
        return result;
    }

    @Override
    public Optional<ICriterion> visitNotCriterion(NotCriterion pCriterion) {
        return pCriterion.getCriterion().accept(this);
    }

    @Override
    public Optional<ICriterion> visitStringMatchCriterion(StringMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitStringMatchAnyCriterion(StringMatchAnyCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitIntMatchCriterion(IntMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitLongMatchCriterion(LongMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <U extends Comparable<? super U>> Optional<ICriterion> visitRangeCriterion(RangeCriterion<U> pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitDateRangeCriterion(DateRangeCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitBooleanMatchCriterion(BooleanMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ICriterion> visitPolygonCriterion(PolygonCriterion pCriterion) {
        return (searchedName.equals(IMapping.GEOMETRY)) ? Optional.of(pCriterion) : Optional.empty();
    }

    @Override
    public Optional<ICriterion> visitCircleCriterion(CircleCriterion pCriterion) {
        return (searchedName.equals(IMapping.GEOMETRY)) ? Optional.of(pCriterion) : Optional.empty();
    }

}
