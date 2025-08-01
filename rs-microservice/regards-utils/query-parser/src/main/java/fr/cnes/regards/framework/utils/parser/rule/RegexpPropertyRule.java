package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A RegexpPropertyRule is a rule that matches properties whose value matches a specific regular expression. If the
 * property is a number or boolean property, its string representation is used for evaluation by the regular expression.
 */
public final class RegexpPropertyRule extends AbstractPropertyRule {

    private final Pattern pattern;

    public RegexpPropertyRule(String property, String regexp) {
        super(property);
        this.pattern = Pattern.compile(regexp);
    }

    public RegexpPropertyRule(String[] propertyPath, Pattern pattern) {
        super(propertyPath);
        this.pattern = pattern;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitRegex(this);
    }

    @Override
    public IRule canonicalize() {
        return this;
    }

    @Override
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        super.toString(sb, parenthesizeIfNeeded);
        sb.append('/').append(pattern.pattern()).append('/');
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RegexpPropertyRule that)) {
            return false;
        }
        return super.equals(that) && pattern.pattern().equals(that.pattern.pattern());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pattern);
    }
}
