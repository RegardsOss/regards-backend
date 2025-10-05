package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.cnes.regards.modules.indexer.dao.EsRepository.ALIAS_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * These tests verify the correct behavior of next building index name resolution logic
 *
 * @author mnguyen0
 */
@ExtendWith(MockitoExtension.class)
public class IndexAliasResolverTest {

    private final String TENANT = "tenant";

    @Mock
    IndexAliasService indexAliasService;

    @InjectMocks
    IndexAliasResolver resolver;

    private EsIndexAlias mockAliasEntry() {
        String alias = TENANT + ALIAS_SUFFIX;
        EsIndexAlias aliasEntry = mock(EsIndexAlias.class);
        when(indexAliasService.getByAlias(alias)).thenReturn(aliasEntry);
        return aliasEntry;
    }

    @Test
    void resolve_next_index_name_increment_test() {
        //Given
        EsIndexAlias aliasEntry = mockAliasEntry();
        String hash = IndexAliasResolver.shortHash(TENANT);
        when(aliasEntry.getBuilding()).thenReturn(IndexAliasResolver.buildIndexName(TENANT, hash, 7));
        //When
        String nextIndexName = resolver.resolveNextIndexName(TENANT);
        // Then
        assertEquals(IndexAliasResolver.buildIndexName(TENANT, hash, 8), nextIndexName);
    }

    @Test
    void resolve_next_index_name_initialization_test() {
        //Given
        EsIndexAlias aliasEntry = mockAliasEntry();
        when(aliasEntry.getBuilding()).thenReturn(null);
        when(aliasEntry.getCurrent()).thenReturn("tenant_index_v42");
        String hash = IndexAliasResolver.shortHash(TENANT);
        //When
        String nextIndexName = resolver.resolveNextIndexName(TENANT);
        // Then
        assertEquals(IndexAliasResolver.buildIndexName(TENANT, hash, 1), nextIndexName);
    }

    @Test
    void resolve_next_index_name_tenant_with_underscores_test() {
        //Given
        String tenant = "tenant_with_underscores";
        String alias = tenant + ALIAS_SUFFIX;
        EsIndexAlias aliasEntry = mock(EsIndexAlias.class);
        when(indexAliasService.getByAlias(alias)).thenReturn(aliasEntry);
        String hash = IndexAliasResolver.shortHash(tenant);
        when(aliasEntry.getBuilding()).thenReturn(IndexAliasResolver.buildIndexName(tenant, hash, 3));
        //When
        String nextIndexName = resolver.resolveNextIndexName(tenant);
        // Then
        assertEquals(IndexAliasResolver.buildIndexName(tenant, hash, 4), nextIndexName);
    }
}
