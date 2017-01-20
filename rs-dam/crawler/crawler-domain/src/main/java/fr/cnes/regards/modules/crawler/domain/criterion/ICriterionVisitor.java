package fr.cnes.regards.modules.crawler.domain.criterion;

/**
 * ICriterion visitor
 * @param <T> return type of all methods
 * @author oroussel
 */
public interface ICriterionVisitor<T> {

    T visitAndCriterion(AndCriterion pCriterion);

    T visitNotCriterion(NotCriterion pCriterion);

    T visitStringMatchCriterion(StringMatchCriterion pCriterion);

    T visitIntMatchCriterion(IntMatchCriterion pCriterion);

    <U> T visitRangeCriterion(RangeCriterion<U> pCriterion);
}
