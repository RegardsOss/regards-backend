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
package fr.cnes.regards.framework.utils.parser;

import fr.cnes.regards.framework.utils.parser.rule.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Julien Canches
 */
public class RuleCanonicalizationTest {

    static final PropertyRule PROP_RULE = new PropertyRule("prop", "value");

    static final RegexpPropertyRule REGEXP_RULE = new RegexpPropertyRule("prop", "a.*b");

    static final NumberRangePropertyRule NUMBER_RANGE_RULE = new NumberRangePropertyRule("prop",
                                                                                         BigDecimal.valueOf(-12),
                                                                                         BigDecimal.valueOf(632),
                                                                                         false,
                                                                                         true);

    static List<Arguments> canonicalize() {
        return List.of(Arguments.of(new OrRule(), IRule.NEVER),
                       Arguments.of(new OrRule(PROP_RULE), PROP_RULE),
                       Arguments.of(new OrRule(IRule.NEVER), IRule.NEVER),
                       Arguments.of(new OrRule(IRule.ALWAYS), IRule.ALWAYS),
                       Arguments.of(new OrRule(PROP_RULE, IRule.NEVER), PROP_RULE),
                       Arguments.of(new OrRule(PROP_RULE, IRule.ALWAYS), IRule.ALWAYS),
                       Arguments.of(new OrRule(PROP_RULE, REGEXP_RULE), new OrRule(PROP_RULE, REGEXP_RULE)),
                       Arguments.of(new AndRule(), IRule.ALWAYS),
                       Arguments.of(new AndRule(PROP_RULE), PROP_RULE),
                       Arguments.of(new AndRule(IRule.NEVER), IRule.NEVER),
                       Arguments.of(new AndRule(IRule.ALWAYS), IRule.ALWAYS),
                       Arguments.of(new AndRule(PROP_RULE, IRule.ALWAYS), PROP_RULE),
                       Arguments.of(new AndRule(PROP_RULE, IRule.NEVER), IRule.NEVER),
                       Arguments.of(new NotRule(new NotRule(PROP_RULE)), PROP_RULE),
                       Arguments.of(new AndRule(PROP_RULE, REGEXP_RULE), new AndRule(PROP_RULE, REGEXP_RULE)),
                       Arguments.of(new AndRule(PROP_RULE, new OrRule()), IRule.NEVER),
                       Arguments.of(new OrRule(PROP_RULE, new AndRule()), IRule.ALWAYS),
                       Arguments.of(PROP_RULE, PROP_RULE),
                       Arguments.of(REGEXP_RULE, REGEXP_RULE),
                       Arguments.of(NUMBER_RANGE_RULE, NUMBER_RANGE_RULE));
    }

    @ParameterizedTest
    @MethodSource
    void canonicalize(IRule rule, IRule expectedCanonicalizedRule) {
        assertThat(rule.canonicalize()).isEqualTo(expectedCanonicalizedRule);
    }
}
