/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.plugin;

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
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.modules.search.domain.assembler.resource.FacettedPagedResources;

/**
 * @author Sylvain Vissiere-Guerinet
 */
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
    public byte[] transform(Collection<AbstractEntity> pEntity, Charset pCharset) {
        StringJoiner sj = new StringJoiner("\n-");
        for (AbstractEntity entity : pEntity) {
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
