package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotRule implements IRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotRule.class);

    private final IRule rule;

    public NotRule(IRule rule) {
        this.rule = rule;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        LOGGER.debug("Accepting {}", this.getClass().getName());
        return visitor.visitNot(this);
    }

    public IRule getRule() {
        return rule;
    }
}
