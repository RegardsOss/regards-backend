/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser;

import fr.cnes.regards.modules.crawler.domain.criterion.AbstractMultiCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.DateRangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.EmptyCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.NotCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchAnyCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class TypeCheckingCriterionVisitor<T> implements ICriterionVisitor<T> {

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitEmptyCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.EmptyCriterion)
     */
    @Override
    public T visitEmptyCriterion(EmptyCriterion pCriterion) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitAndCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.AbstractMultiCriterion)
     */
    @Override
    public T visitAndCriterion(AbstractMultiCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitOrCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.AbstractMultiCriterion)
     */
    @Override
    public T visitOrCriterion(AbstractMultiCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitNotCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.NotCriterion)
     */
    @Override
    public T visitNotCriterion(NotCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitStringMatchCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.StringMatchCriterion)
     */
    @Override
    public T visitStringMatchCriterion(StringMatchCriterion pCriterion) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitStringMatchAnyCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.StringMatchAnyCriterion)
     */
    @Override
    public T visitStringMatchAnyCriterion(StringMatchAnyCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitIntMatchCriterion(fr.cnes.regards.modules
     * .crawler.domain.criterion.IntMatchCriterion)
     */
    @Override
    public T visitIntMatchCriterion(IntMatchCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitRangeCriterion(fr.cnes.regards.modules.
     * crawler.domain.criterion.RangeCriterion)
     */
    @Override
    public <U extends Comparable<? super U>> T visitRangeCriterion(RangeCriterion<U> pCriterion) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitDateRangeCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.DateRangeCriterion)
     */
    @Override
    public T visitDateRangeCriterion(DateRangeCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.crawler.domain.criterion.ICriterionVisitor#visitBooleanMatchCriterion(fr.cnes.regards.
     * modules.crawler.domain.criterion.BooleanMatchCriterion)
     */
    @Override
    public T visitBooleanMatchCriterion(BooleanMatchCriterion pCriterion) {
        // TODO Auto-generated method stub
        return null;
    }

}
