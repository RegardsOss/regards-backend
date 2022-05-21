package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class RegexpPropertyRule implements IRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegexpPropertyRule.class);

    private final String property;

    private final Pattern pattern;

    public RegexpPropertyRule(String property, String regexp) {
        this.property = property;
        this.pattern = Pattern.compile(regexp);
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        LOGGER.debug("Accepting {}", this.getClass().getName());
        return visitor.visitRegex(this);
    }

    public String getProperty() {
        return property;
    }

    public Pattern getPattern() {
        return pattern;
    }

}
