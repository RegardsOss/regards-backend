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

import fr.cnes.regards.framework.utils.parser.IRuleVisitor;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A NumberRangePropertyRule is a rule that matches number properties whose value belongs to a specific interval. It is
 * possible to omit either the lower or upper bound (making the rule respectively a "is lower than" or "is greater
 * than" rule), and each bound may be either inclusive or exclusive.
 *
 * @author Julien Canches
 */
public final class NumberRangePropertyRule extends AbstractPropertyRule {

    private final @Nullable BigDecimal lowerBound;

    private final @Nullable BigDecimal upperBound;

    private final boolean lowerInclusive;

    private final boolean upperInclusive;

    public NumberRangePropertyRule(String property,
                                   @Nullable BigDecimal lowerBound,
                                   @Nullable BigDecimal upperBound,
                                   boolean lowerInclusive,
                                   boolean upperInclusive) {
        super(property);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
    }

    public NumberRangePropertyRule(String[] propertyPath,
                                   @Nullable BigDecimal lowerBound,
                                   @Nullable BigDecimal upperBound,
                                   boolean lowerInclusive,
                                   boolean upperInclusive) {
        super(propertyPath);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
    }

    @Override
    public <U> U accept(IRuleVisitor<U> visitor) {
        return visitor.visitNumberRange(this);
    }

    @Override
    public IRule canonicalize() {
        return this;
    }

    @Override
    @SuppressWarnings("java:S1067") // This method has more than 3 conditional operators, splitting it would actually
    // make the whole class more complex
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        super.toString(sb, parenthesizeIfNeeded);
        sb.append(lowerInclusive ? '[' : '{')
          .append(lowerBound == null ? "*" : lowerBound)
          .append(" TO ")
          .append(upperBound == null ? "*" : upperBound)
          .append(upperInclusive ? ']' : '}');
    }

    public @Nullable BigDecimal getLowerBound() {
        return lowerBound;
    }

    public @Nullable BigDecimal getUpperBound() {
        return upperBound;
    }

    public boolean isLowerInclusive() {
        return lowerInclusive;
    }

    public boolean isUpperInclusive() {
        return upperInclusive;
    }

    public boolean matchesValue(BigDecimal value) {
        int lowerCompare = lowerBound == null ? -1 : lowerBound.compareTo(value);
        int upperCompare = upperBound == null ? 1 : upperBound.compareTo(value);
        // @formatter:off
        return (lowerInclusive ? lowerCompare <= 0 : lowerCompare < 0) &&
               (upperInclusive ? upperCompare >= 0 : upperCompare > 0);
        // @formatter:on
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NumberRangePropertyRule that)) {
            return false;
        }
        return lowerInclusive == that.lowerInclusive
               && upperInclusive == that.upperInclusive
               && super.equals(that)
               && Objects.equals(lowerBound, that.lowerBound)
               && Objects.equals(upperBound, that.upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lowerBound, upperBound, lowerInclusive, upperInclusive);
    }
}
