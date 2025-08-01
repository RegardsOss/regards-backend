package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

import java.util.Objects;

/**
 * A PropertyRule is a rule that matches properties whose value is equal to a specific value. The value is compared
 * as a string, i.e. if the property is a number or boolean property, its string representation is used for comparison.
 */
public final class PropertyRule extends AbstractPropertyRule {

    private final String value;

    public PropertyRule(String property, String value) {
        super(property);
        this.value = value;
    }

    public PropertyRule(String[] propertyPath, String value) {
        super(propertyPath);
        this.value = value;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitProperty(this);
    }

    @Override
    public IRule canonicalize() {
        return this;
    }

    @Override
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        super.toString(sb, parenthesizeIfNeeded);
        sb.append(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyRule that)) {
            return false;
        }
        return super.equals(o) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
