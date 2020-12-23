package fr.cnes.regards.framework.utils.parser;

import fr.cnes.regards.framework.utils.parser.rule.AndRule;
import fr.cnes.regards.framework.utils.parser.rule.NotRule;
import fr.cnes.regards.framework.utils.parser.rule.PropertyRule;
import fr.cnes.regards.framework.utils.parser.rule.RegexpPropertyRule;

public interface IRuleVisitor<T> {

    T visit(AndRule rule);

    T visit(NotRule rule);

    T visit(PropertyRule rule);

    T visit(RegexpPropertyRule rule);
}
