package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

import java.util.List;

/**
 * An OrRule is a rule that consists of multiple sub-rules, and that is satisfied if any one of its sub-rules is
 * satisfied.
 */
public class OrRule extends AbstractCompositeRule {

    public OrRule(IRule... rules) {
        super(rules);
    }

    public OrRule(List<IRule> rules) {
        super(rules);
    }

    @Override
    protected boolean isNeutralRule(IRule rule) {
        return rule == IRule.NEVER;
    }

    @Override
    protected boolean isOverriddenByRule(IRule rule) {
        return rule == IRule.ALWAYS;
    }

    @Override
    protected IRule canonicalizeEmpty() {
        return IRule.NEVER;
    }

    @Override
    protected IRule with(List<IRule> rules) {
        return new OrRule(rules);
    }

    @Override
    protected String getOperatorAsString() {
        return "OR";
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitOr(this);
    }

}
