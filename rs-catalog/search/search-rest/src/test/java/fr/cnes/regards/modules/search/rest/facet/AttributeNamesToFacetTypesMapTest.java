/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.facet;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.thoughtworks.xstream.converters.ConversionException;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.search.rest.CatalogControllerTestUtils;
import fr.cnes.regards.modules.search.rest.converter.AttributeNamesToFacetTypesMap;
import fr.cnes.regards.modules.search.service.cache.attributemodel.IAttributeModelCache;

/**
 * Unit test for {@link AttributeNamesToFacetTypesMap}.
 * @author Xavier-Alexandre Brochard
 */
public class AttributeNamesToFacetTypesMapTest {

    /**
     * Class under test
     */
    private AttributeNamesToFacetTypesMap converter;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        IAttributeModelCache attributeModelCache = Mockito.mock(IAttributeModelCache.class);
        Mockito.when(attributeModelCache.findByName(CatalogControllerTestUtils.STRING_ATTRIBUTE_NAME))
                .thenReturn(CatalogControllerTestUtils.STRING_ATTRIBUTE_MODEL);
        Mockito.when(attributeModelCache.findByName(CatalogControllerTestUtils.INTEGER_ATTRIBUTE_NAME))
                .thenReturn(CatalogControllerTestUtils.INTEGER_ATTRIBUTE_MODEL);
        Mockito.when(attributeModelCache.findByName(CatalogControllerTestUtils.DATE_ATTRIBUTE_NAME))
                .thenReturn(CatalogControllerTestUtils.DATE_ATTRIBUTE_MODEL);
        Mockito.when(attributeModelCache.findByName(CatalogControllerTestUtils.UNEXISTNG_ATTRIBUTE_NAME))
                .thenThrow(ConversionException.class);

        converter = new AttributeNamesToFacetTypesMap(attributeModelCache);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.converter.AttributeNamesToFacetTypesMap#convert(java.lang.String[])}.
     */
    @Test
    @Purpose("The converter should properly convert type of string into a map of facet type")
    public final void testConvert_shouldConvertIntoExpectedResult() {
        // Define expected conversion result
        Map<String, FacetType> expected = new HashMap<>();
        expected.put(CatalogControllerTestUtils.STRING_ATTRIBUTE_NAME, FacetType.STRING);
        expected.put(CatalogControllerTestUtils.INTEGER_ATTRIBUTE_NAME, FacetType.NUMERIC);
        expected.put(CatalogControllerTestUtils.DATE_ATTRIBUTE_NAME, FacetType.DATE);

        Assert.assertEquals(expected, converter.convert(CatalogControllerTestUtils.FACETS_AS_ARRAY));
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.converter.AttributeNamesToFacetTypesMap#convert(java.lang.String[])}.
     */
    @Test(expected = ConversionException.class)
    @Purpose("The converter should throw a conversion exception when attribute does not exist")
    public final void testConvert_shouldThrowAConversionExceptionWhenAttributeDoesNotExist() {
        // Define an array with unexisting attributes
        String[] facets = { CatalogControllerTestUtils.UNEXISTNG_ATTRIBUTE_NAME };
        converter.convert(facets);
    }

}
