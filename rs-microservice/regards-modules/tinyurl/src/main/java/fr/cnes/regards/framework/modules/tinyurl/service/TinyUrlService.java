/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.tinyurl.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.tinyurl.dao.TinyUrlRepository;
import fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing {@link fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl}
 *
 * @author Marc SORDI
 */
@Service
@MultitenantTransactional
public class TinyUrlService {

    private final TinyUrlRepository tinyUrlRepository;

    private final Gson gson;

    public TinyUrlService(TinyUrlRepository tinyUrlRepository, Gson gson) {
        this.tinyUrlRepository = tinyUrlRepository;
        this.gson = gson;
    }

    /**
     * Create a new tiny URL storing its context
     *
     * @param context        context of the tiny URL
     * @param classOfContext class representing the context
     * @param lifetime       lifetime in hour
     * @return a new {@link TinyUrl}
     */
    private TinyUrl create(JsonElement context, Class<?> classOfContext, long lifetime) {
        // Create tinyurl
        TinyUrl tinyUrl = new TinyUrl();
        tinyUrl.setUuid(UUID.randomUUID().toString());
        tinyUrl.setContext(context);
        tinyUrl.setClassOfContext(classOfContext.getName());
        tinyUrl.setExpirationDate(OffsetDateTime.now().plusHours(lifetime));
        // Store it
        return tinyUrlRepository.save(tinyUrl);
    }

    /**
     * Create a new tiny URL converting passed context to JSON element and storing it.
     *
     * @param context  context of the tiny URL to be converted to JSON element
     * @param lifetime lifetime in hour
     * @return a new {@link TinyUrl}
     */
    public TinyUrl create(Object context, long lifetime) {
        if (JsonElement.class.isAssignableFrom(context.getClass())) {
            return create((JsonElement) context, JsonElement.class, lifetime);
        }
        return create(gson.toJsonTree(context), context.getClass(), lifetime);
    }

    /**
     * Create a new tiny URL converting passed context to JSON element and storing it with a 24h lifetime.
     *
     * @param context context of the tiny URL to be converted to JSON element
     * @return a new {@link TinyUrl}
     */
    public TinyUrl create(Object context) {
        return create(context, 24);
    }

    /**
     * Retrieve context from tiny URL id
     *
     * @param uuid tiny URL id
     * @return an optional {@link TinyUrl}
     */
    public Optional<TinyUrl> get(String uuid) {
        return tinyUrlRepository.findByUuid(uuid);
    }

    /**
     * Explicit removal of the tiny URL
     *
     * @param uuid tiny URL identifier
     */
    public void delete(String uuid) {
        tinyUrlRepository.deleteByUuid(uuid);
    }

    /**
     * Load context from JSON
     *
     * @param tinyUrl tiny URL with context to load
     * @param type    target type to load in
     * @param <T>     target type
     * @return a loaded context
     */
    public <T> T loadContext(TinyUrl tinyUrl, Type type) {
        return gson.fromJson(tinyUrl.getContext(), type);
    }

    /**
     * Purge tiny URL
     */
    public void purge() {
        tinyUrlRepository.deleteByExpirationDateLessThan(OffsetDateTime.now());
    }
}
