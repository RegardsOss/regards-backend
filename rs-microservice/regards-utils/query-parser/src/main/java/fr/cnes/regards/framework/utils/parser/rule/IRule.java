package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

/**
 * A rule expresses a condition. It can accept a {@link IRuleVisitor visitor} to explore the details of the rule.
 * <p>
 * This interface is not meant to be implemented by consumers.
 */
public interface IRule {

    /**
     * A special rule that never matches anything.
     */
    IRule NEVER = new AbstractRule() {

        @Override
        public <U> U accept(IRuleVisitor<U> visitor) {
            return visitor.visitNever();
        }

        @Override
        public IRule canonicalize() {
            return this;
        }

        @Override
        protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
            sb.append("FALSE");
        }
    };

    /**
     * A special rule that always matches everything.
     */
    IRule ALWAYS = new AbstractRule() {

        @Override
        public <U> U accept(IRuleVisitor<U> visitor) {
            return visitor.visitAlways();
        }

        @Override
        public IRule canonicalize() {
            return this;
        }

        @Override
        protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
            sb.append("TRUE");
        }
    };

    <U> U accept(IRuleVisitor<U> visitor);

    /**
     * Returns an identical but potentially simplified rule. For instance, and/or groups comprised of a single
     * sub-rule are substituted by their unique sub-rule. And/or groups that always evaluate to false or true are also
     * replaced by a static rule.
     * <p>
     * It is recommended to canonicalize rules once if you intend to apply them frequently.
     *
     * @return The substitute and simplified rule. This may return the same rule.
     */
    IRule canonicalize();

}
