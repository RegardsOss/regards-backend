/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.utils.parser.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A CompositeRule is a rule that consists of several sub-rules. This rule is the base abstraction for {@link AndRule}
 * and {@link OrRule}.
 *
 * @author Julien Canches
 */
public abstract class AbstractCompositeRule extends AbstractRule {

    protected final List<IRule> rules;

    protected AbstractCompositeRule(IRule... rules) {
        this.rules = List.of(rules);
    }

    protected AbstractCompositeRule(List<IRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Returns whether adding or removing the specified sub-rule from this rule would not affect the current rule.
     */
    protected abstract boolean isNeutralRule(IRule rule);

    /**
     * Returns whether adding the specified sub-rule to this rule would make this rule equivalent to the specified rule.
     */
    protected abstract boolean isOverriddenByRule(IRule rule);

    /**
     * Returns the equivalent rule when there are no sub-rules.
     */
    protected abstract IRule canonicalizeEmpty();

    /**
     * Returns a composite rule with the same operator and the specified sub-rules.
     */
    protected abstract IRule with(List<IRule> rules);

    @Override
    public IRule canonicalize() {
        List<IRule> canonicalized = new ArrayList<>();
        for (IRule rule : rules) {
            IRule cr = rule.canonicalize();
            if (isOverriddenByRule(cr)) {
                return cr;
            }
            if (!isNeutralRule(cr)) {
                canonicalized.add(cr);
            }
        }
        if (canonicalized.isEmpty()) {
            return canonicalizeEmpty();
        }
        if (canonicalized.size() == 1) {
            return canonicalized.get(0);
        }
        return with(canonicalized);
    }

    protected abstract String getOperatorAsString();

    @Override
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        if (rules.size() == 1) {
            ((AbstractRule) rules.get(0)).toString(sb, parenthesizeIfNeeded);
        } else {
            if (parenthesizeIfNeeded) {
                sb.append('(');
            }
            String separator = ' ' + getOperatorAsString() + ' ';
            boolean op = false;
            for (IRule r : rules) {
                if (op) {
                    sb.append(separator);
                } else {
                    op = true;
                }
                ((AbstractRule) r).toString(sb, true);
            }
            if (parenthesizeIfNeeded) {
                sb.append(')');
            }
        }
    }

    public List<IRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractCompositeRule that = (AbstractCompositeRule) o;
        return Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rules);
    }
}
