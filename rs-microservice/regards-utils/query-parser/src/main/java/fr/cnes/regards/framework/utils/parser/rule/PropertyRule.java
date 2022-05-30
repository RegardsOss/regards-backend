package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyRule implements IRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyRule.class);

    private final String property;

    private final String value;

    public PropertyRule(String property, String value) {
        this.property = property;
        this.value = value;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        LOGGER.debug("Accepting {}", this.getClass().getName());
        return visitor.visitProperty(this);
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
}
