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
package fr.cnes.regards.modules.processing.entity.callback;

import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * EntityCallback that automatically sets the persisted flag on {@link ExecutionEntity} instances after they are
 * inserted into the database, or read back from the database. Having the persisted flag set to true ensures that
 * Spring performs an UPDATE rather than an INSERT on entities that are already persisted (cf the method
 * {@link ExecutionEntity#isNew()} implementing {@link org.springframework.data.domain.Persistable}).
 *
 * @author Julien Canches
 */
@Component
public class ExecutionEntityCallback
    implements AfterSaveCallback<ExecutionEntity>, AfterConvertCallback<ExecutionEntity> {

    /**
     * Entity callback method invoked after save to set its persisted flag to true.
     */
    @Override
    public Publisher<ExecutionEntity> onAfterSave(ExecutionEntity entity, OutboundRow row, SqlIdentifier table) {
        return Mono.create(sink -> sink.success(entity.persisted()));
    }

    /**
     * Entity callback method invoked after load (converted from database row) to set its persisted flag to true.
     */
    @Override
    public Publisher<ExecutionEntity> onAfterConvert(ExecutionEntity entity, SqlIdentifier table) {
        return Mono.create(sink -> sink.success(entity.persisted()));
    }

}
