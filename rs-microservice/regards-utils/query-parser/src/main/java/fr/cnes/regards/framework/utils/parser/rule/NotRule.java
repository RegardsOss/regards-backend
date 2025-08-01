package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

import java.util.Objects;

/**
 * A NotRule is a rule that negates another rule, i.e. that is satisfied if and only if the other rule is not
 * satisfied.
 */
public final class NotRule extends AbstractRule {

    private final IRule rule;

    public NotRule(IRule rule) {
        this.rule = rule;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitNot(this);
    }

    @Override
    public IRule canonicalize() {
        if (rule instanceof NotRule not) {
            // (NOT (NOT a)) == a
            return not.getRule();
        }
        return new NotRule(rule.canonicalize());
    }

    @Override
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        sb.append("NOT ");
        ((AbstractRule) rule).toString(sb, true);
    }

    public IRule getRule() {
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotRule notRule)) {
            return false;
        }
        return Objects.equals(rule, notRule.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rule);
    }
}
