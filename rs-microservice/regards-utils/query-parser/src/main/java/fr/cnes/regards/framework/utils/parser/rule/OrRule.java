package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OrRule implements IRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrRule.class);

    private final List<IRule> rules = new ArrayList<>();

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        LOGGER.debug("Accepting {}", this.getClass().getName());
        return visitor.visitOr(this);
    }

    public void add(IRule rule) {
        rules.add(rule);
    }

    public List<IRule> getRules() {
        return rules;
    }
}
