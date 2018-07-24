/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.indexer.domain.IMapping;
import fr.cnes.regards.modules.indexer.domain.criterion.AbstractMultiCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.BoundaryBoxCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.CircleCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.DateMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.DateRangeCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.EmptyCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.FieldExistsCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterionVisitor;
import fr.cnes.regards.modules.indexer.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.LongMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.NotCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchAnyCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;

/**
 * Criterion visitor which tries to find all ICriterion concerning the specified attribute name.<br>
 * Returns a {@link Collection} of criterion.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NamedCriterionFinderVisitor implements ICriterionVisitor<Collection<ICriterion>> {

    /**
     * The name of the criterion we are looking for
     */
    private final String searchedName;

    /**
     * @param pSearchedName the name of the searched criterion
     */
    public NamedCriterionFinderVisitor(String pSearchedName) {
        super();
        searchedName = pSearchedName;
    }

    @Override
    public Collection<ICriterion> visitEmptyCriterion(EmptyCriterion criterion) {
        return new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitAndCriterion(AbstractMultiCriterion criterion) {
        Collection<ICriterion> result = new ArrayList<>();
        Iterator<ICriterion> criterionIterator = criterion.getCriterions().iterator();
        while (criterionIterator.hasNext()) {
            result.addAll(criterionIterator.next().accept(this));
        }
        return result;
    }

    @Override
    public Collection<ICriterion> visitOrCriterion(AbstractMultiCriterion criterion) {
        Collection<ICriterion> result = new ArrayList<>();
        Iterator<ICriterion> criterionIterator = criterion.getCriterions().iterator();
        while (criterionIterator.hasNext()) {
            result.addAll(criterionIterator.next().accept(this));
        }
        return result;
    }

    @Override
    public Collection<ICriterion> visitNotCriterion(NotCriterion criterion) {
        return criterion.getCriterion().accept(this);
    }

    @Override
    public Collection<ICriterion> visitStringMatchCriterion(StringMatchCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitIntMatchCriterion(IntMatchCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitLongMatchCriterion(LongMatchCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitDateMatchCriterion(DateMatchCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public <U extends Comparable<? super U>> Collection<ICriterion> visitRangeCriterion(RangeCriterion<U> criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitDateRangeCriterion(DateRangeCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitBooleanMatchCriterion(BooleanMatchCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitPolygonCriterion(PolygonCriterion criterion) {
        return searchedName.equals(IMapping.GEO_SHAPE_ATTRIBUTE) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitCircleCriterion(CircleCriterion criterion) {
        return searchedName.equals(IMapping.GEO_SHAPE_ATTRIBUTE) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitFieldExistsCriterion(FieldExistsCriterion criterion) {
        return searchedName.equals(criterion.getName()) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

    @Override
    public Collection<ICriterion> visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {
        return searchedName.equals(IMapping.GEO_SHAPE_ATTRIBUTE) ? Lists.newArrayList(criterion) : new ArrayList<>();
    }

}
