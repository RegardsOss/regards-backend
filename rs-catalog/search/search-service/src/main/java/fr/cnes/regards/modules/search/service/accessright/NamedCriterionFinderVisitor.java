/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.assertj.core.util.Lists;

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
 * Criterion visitor which tries to find all ICriterion concerning the specified attribute name.<br>
 * Returns a {@link Collection} of criterion.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NamedCriterionFinderVisitor implements ICriterionVisitor<Collection<ICriterion>> {

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
    public Collection<ICriterion> visitEmptyCriterion(EmptyCriterion pCriterion) {
        return new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitAndCriterion(AbstractMultiCriterion pCriterion) {
        Collection<ICriterion> result = new ArrayList<>();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (criterionIterator.hasNext()) {
            result.addAll(criterionIterator.next().accept(this));
        }
        return result;
    }

    @Override
    public Collection<ICriterion> visitOrCriterion(AbstractMultiCriterion pCriterion) {
        Collection<ICriterion> result = new ArrayList<>();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (criterionIterator.hasNext()) {
            result.addAll(criterionIterator.next().accept(this));
        }
        return result;
    }

    @Override
    public Collection<ICriterion> visitNotCriterion(NotCriterion pCriterion) {
        return pCriterion.getCriterion().accept(this);
    }

    @Override
    public Collection<ICriterion> visitStringMatchCriterion(StringMatchCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitStringMatchAnyCriterion(StringMatchAnyCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitIntMatchCriterion(IntMatchCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitLongMatchCriterion(LongMatchCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public <U extends Comparable<? super U>> Collection<ICriterion> visitRangeCriterion(RangeCriterion<U> pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitDateRangeCriterion(DateRangeCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitBooleanMatchCriterion(BooleanMatchCriterion pCriterion) {
        return searchedName.equals(pCriterion.getName()) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitPolygonCriterion(PolygonCriterion pCriterion) {
        return searchedName.equals(IMapping.GEOMETRY) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitCircleCriterion(CircleCriterion pCriterion) {
        return searchedName.equals(IMapping.GEOMETRY) ? Lists.newArrayList(pCriterion) : new ArrayList<>();
    }

}
