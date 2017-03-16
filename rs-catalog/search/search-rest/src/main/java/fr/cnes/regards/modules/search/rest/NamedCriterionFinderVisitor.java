/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.Iterator;
import java.util.Optional;

import fr.cnes.regards.modules.crawler.domain.criterion.AbstractMultiCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.DateRangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.EmptyCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.LongMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.NotCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchAnyCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;

/**
 * Criterion visitor which tries to find a {@link StringMatchCriterion} with name "target".<br>
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
     * @param pSearchedName
     *            the name of the searched criterion
     */
    public NamedCriterionFinderVisitor(String pSearchedName) {
        super();
        searchedName = pSearchedName;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitEmptyCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.EmptyCriterion)
     */
    @Override
    public Optional<ICriterion> visitEmptyCriterion(EmptyCriterion pCriterion) {
        return Optional.empty();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitAndCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.AbstractMultiCriterion)
     */
    @Override
    public Optional<ICriterion> visitAndCriterion(AbstractMultiCriterion pCriterion) {
        Optional<ICriterion> result = Optional.empty();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (!result.isPresent() && criterionIterator.hasNext()) {
            result = criterionIterator.next().accept(this);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitOrCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.AbstractMultiCriterion)
     */
    @Override
    public Optional<ICriterion> visitOrCriterion(AbstractMultiCriterion pCriterion) {
        Optional<ICriterion> result = Optional.empty();
        Iterator<ICriterion> criterionIterator = pCriterion.getCriterions().iterator();
        while (!result.isPresent() && criterionIterator.hasNext()) {
            result = criterionIterator.next().accept(this);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitNotCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.NotCriterion)
     */
    @Override
    public Optional<ICriterion> visitNotCriterion(NotCriterion pCriterion) {
        return pCriterion.getCriterion().accept(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitStringMatchCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.StringMatchCriterion)
     */
    @Override
    public Optional<ICriterion> visitStringMatchCriterion(StringMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitStringMatchAnyCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.StringMatchAnyCriterion)
     */
    @Override
    public Optional<ICriterion> visitStringMatchAnyCriterion(StringMatchAnyCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitIntMatchCriterion(fr.cnes.regards.modules
     * .crawler.domain.criterion.IntMatchCriterion)
     */
    @Override
    public Optional<ICriterion> visitIntMatchCriterion(IntMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitLongMatchCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.LongMatchCriterion)
     */
    @Override
    public Optional<ICriterion> visitLongMatchCriterion(LongMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitRangeCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.RangeCriterion)
     */
    @Override
    public <U extends Comparable<? super U>> Optional<ICriterion> visitRangeCriterion(RangeCriterion<U> pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitDateRangeCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.DateRangeCriterion)
     */
    @Override
    public Optional<ICriterion> visitDateRangeCriterion(DateRangeCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitBooleanMatchCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.BooleanMatchCriterion)
     */
    @Override
    public Optional<ICriterion> visitBooleanMatchCriterion(BooleanMatchCriterion pCriterion) {
        if (searchedName.equals(pCriterion.getName())) {
            return Optional.of(pCriterion);
        } else {
            return Optional.empty();
        }
    }

}
