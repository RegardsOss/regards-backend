package fr.cnes.regards.framework.utils.parser;

import fr.cnes.regards.framework.utils.parser.rule.*;

public interface IRuleVisitor<T> {

    T visitAnd(AndRule rule);

    T visitOr(OrRule rule);

    T visitNot(NotRule rule);

    T visitProperty(PropertyRule rule);

    T visitRegex(RegexpPropertyRule rule);
}
