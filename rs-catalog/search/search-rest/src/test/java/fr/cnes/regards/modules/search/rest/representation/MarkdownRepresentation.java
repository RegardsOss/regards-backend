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
package fr.cnes.regards.modules.search.rest.representation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedResources;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Deprecated // Use search engine instead
@Plugin(author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "represnetation plugin for test purpose, doesn't even really serialize to markdown",
        id = "MarkdownRepresentation", licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "0.0.1")
public class MarkdownRepresentation implements IRepresentation {

    public static final MediaType MEDIA_TYPE = new MediaType("text", "markdown", StandardCharsets.UTF_8);

    @Override
    public MediaType getHandledMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public byte[] transform(AbstractEntity pToBeTransformed, Charset pTargetCharset) {
        String toSend = stringify(pToBeTransformed);
        return toSend.getBytes(pTargetCharset);
    }

    private String stringify(AbstractEntity pToBeTransformed) {
        StringJoiner sj = new StringJoiner("\n");
        sj.add("label: " + pToBeTransformed.getLabel());
        sj.add("ipId: " + pToBeTransformed.getIpId());
        String toSend = sj.toString();
        return toSend;
    }

    @Override
    public byte[] transform(Collection<AbstractEntity> entities, Charset pCharset) {
        StringJoiner sj = new StringJoiner("\n-");
        for (AbstractEntity entity : entities) {
            sj.add(stringify(entity));
        }
        return sj.toString().getBytes(pCharset);
    }

    @Override
    public byte[] transform(PagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset) {
        return transform(pEntity.getContent().stream().map(r -> r.getContent()).collect(Collectors.toList()), pCharset);
    }

    @Override
    public byte[] transform(FacettedPagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset) {
        return transform(pEntity.getContent().stream().map(r -> r.getContent()).collect(Collectors.toList()), pCharset);
    }

    @Override
    public byte[] transform(Resource<AbstractEntity> pEntity, Charset pCharset) {
        return transform(pEntity.getContent(), pCharset);
    }

}
