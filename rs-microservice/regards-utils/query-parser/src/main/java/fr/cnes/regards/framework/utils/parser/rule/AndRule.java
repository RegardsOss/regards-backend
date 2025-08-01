package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

import java.util.List;

/**
 * An AndRule is a rule that consists of multiple sub-rules, and that is satisfied only if all of its sub-rules are
 * satisfied.
 */
public class AndRule extends AbstractCompositeRule {

    public AndRule(IRule... rules) {
        super(rules);
    }

    public AndRule(List<IRule> rules) {
        super(rules);
    }

    @Override
    protected boolean isNeutralRule(IRule rule) {
        return rule == IRule.ALWAYS;
    }

    @Override
    protected boolean isOverriddenByRule(IRule rule) {
        return rule == IRule.NEVER;
    }

    @Override
    protected IRule canonicalizeEmpty() {
        return IRule.ALWAYS;
    }

    @Override
    protected IRule with(List<IRule> rules) {
        return new AndRule(rules);
    }

    @Override
    protected String getOperatorAsString() {
        return "AND";
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitAnd(this);
    }

}
