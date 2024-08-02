/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.dao.repository;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.authentication.dao.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ServiceProviderRepositoryImpl implements IServiceProviderRepository {

    private final IServiceProviderEntityRepository delegate;

    private final DomainEntityMapper mapper;

    @Autowired
    public ServiceProviderRepositoryImpl(IServiceProviderEntityRepository delegate, DomainEntityMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Option<ServiceProvider> findByName(String name) {
        return Option.ofOptional(delegate.findOneByName(name)).map(mapper::toDomain);
    }

    @Override
    public Page<ServiceProvider> findAll(Pageable pageable) {
        return delegate.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<ServiceProvider> findAll() {
        return List.ofAll(delegate.findAll()).map(mapper::toDomain);
    }

    @Override
    public ServiceProvider save(ServiceProvider serviceProvider) {
        return mapper.toDomain(delegate.save(mapper.toEntity(serviceProvider)));
    }

    @Override
    public Unit delete(String name) {
        delegate.deleteByName(name);
        return Unit.UNIT;
    }

    @VisibleForTesting
    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}
