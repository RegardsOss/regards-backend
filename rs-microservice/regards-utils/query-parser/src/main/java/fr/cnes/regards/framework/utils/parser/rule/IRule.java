package fr.cnes.regards.framework.utils.parser.rule;

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;

@FunctionalInterface
public interface IRule {

    <U> U accept(IRuleVisitor<U> visitor);
}
