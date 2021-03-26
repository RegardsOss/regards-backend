package fr.cnes.regards.framework.utils.parser;

import fr.cnes.regards.framework.utils.parser.rule.AndRule;
import fr.cnes.regards.framework.utils.parser.rule.NotRule;
import fr.cnes.regards.framework.utils.parser.rule.OrRule;
import fr.cnes.regards.framework.utils.parser.rule.PropertyRule;
import fr.cnes.regards.framework.utils.parser.rule.RegexpPropertyRule;

public interface IRuleVisitor<T> {

    T visitAnd(AndRule rule);

    T visitOr(OrRule rule);

    T visitNot(NotRule rule);

    T visitProperty(PropertyRule rule);

    T visitRegex(RegexpPropertyRule rule);
}
