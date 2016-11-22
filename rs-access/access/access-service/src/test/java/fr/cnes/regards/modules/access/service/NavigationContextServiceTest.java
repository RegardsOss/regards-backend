/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.dao.INavigationContextRepository;
import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;

/**
 * 
 * @author Christophe Mertz
 *
 */

public class NavigationContextServiceTest extends NavigationContextUtility {

    /**
     * A context identifier
     */
    private final Long ctxId = 999L;

    /**
     * A context identifier
     */
    private final Integer storeId = 12345;

    /**
     * 
     */
    private INavigationContextRepository nvgCtxRepositoryMocked;

    /**
     * 
     */
    private INavigationContextService nvgCtxServiceMocked;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() {
        // create a mock repository
        nvgCtxRepositoryMocked = Mockito.mock(INavigationContextRepository.class);
        nvgCtxServiceMocked = new NavigationContextService(nvgCtxRepositoryMocked);
    }

    @Test
    public void list() {
        Mockito.when(nvgCtxRepositoryMocked.findAll()).thenReturn(getContextsIterale());
        final List<NavigationContext> contexts = nvgCtxServiceMocked.list();

        Assert.assertNotNull(contexts);
        Assert.assertFalse(contexts.isEmpty());
        Assert.assertEquals(getContexts().size(), contexts.size());
    }

    @Test
    public void update() {
        NavigationContext updateNvgCtx = null;
        Mockito.when(nvgCtxRepositoryMocked.exists(navCtx2.getId())).thenReturn(true);
        Mockito.when(nvgCtxRepositoryMocked.save(navCtx2)).thenReturn(navCtx2);
        try {
            navCtx2.setRoute("modified route");
            navCtx2.setStore(storeId);
            navCtx2.addQueryParameters(new ConfigParameter("param1", "value1"));
            navCtx2.addQueryParameters(new ConfigParameter("param2", "value2"));
            navCtx2.setTinyUrl("hello/NewYork");

            updateNvgCtx = nvgCtxServiceMocked.update(navCtx2);
        } catch (EntityNotFoundException e) {
            Assert.fail();
        }

        Assert.assertNotNull(updateNvgCtx);
        Assert.assertEquals(navCtx2.getTinyUrl(), updateNvgCtx.getTinyUrl());
        Assert.assertEquals(navCtx2.getRoute(), updateNvgCtx.getRoute());
        Assert.assertEquals(navCtx2.getProject(), updateNvgCtx.getProject());
        Assert.assertEquals(navCtx2.getStore(), updateNvgCtx.getStore());
        Assert.assertEquals(navCtx2.getQueryParameters().isEmpty(), updateNvgCtx.getQueryParameters().isEmpty());
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateUnknownContext() throws EntityNotFoundException {
        Mockito.when(nvgCtxRepositoryMocked.exists(navCtx2.getId())).thenReturn(false);

        navCtx2.setRoute("modified route");
        navCtx2.setStore(storeId);
        navCtx2.addQueryParameters(new ConfigParameter("param1", "value1"));
        navCtx2.addQueryParameters(new ConfigParameter("param2", "value2"));
        navCtx2.setTinyUrl("hello/NewYork");

        nvgCtxServiceMocked.update(navCtx2);

        Assert.fail();
    }

    @Test
    public void load() {
        Mockito.when(nvgCtxRepositoryMocked.findOne(ctxId)).thenReturn(navCtx2);
        NavigationContext context = null;
        try {
            context = nvgCtxServiceMocked.load(ctxId);
        } catch (EntityNotFoundException e) {
            Assert.fail();
        }
        Assert.assertEquals(navCtx2.getTinyUrl(), context.getTinyUrl());
        Assert.assertEquals(navCtx2.getRoute(), context.getRoute());
        Assert.assertEquals(navCtx2.getProject(), context.getProject());
        Assert.assertEquals(navCtx2.getStore(), context.getStore());

        Assert.assertNotNull(context);
    }

    @Test(expected = EntityNotFoundException.class)
    public void loadUnknownContex() throws EntityNotFoundException {
        Mockito.when(nvgCtxRepositoryMocked.findOne(ctxId)).thenReturn(null);
        nvgCtxServiceMocked.load(ctxId);
        Assert.fail();
    }

    @Test
    public void delete() {
        Mockito.when(nvgCtxRepositoryMocked.findOne(ctxId)).thenReturn(navCtx2);
        try {
            nvgCtxServiceMocked.delete(ctxId);
        } catch (EntityNotFoundException e) {
            Assert.fail();
        }
        Assert.assertTrue(true);
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteUnknwon() throws EntityNotFoundException {
        Mockito.when(nvgCtxRepositoryMocked.findOne(ctxId)).thenReturn(null);
        nvgCtxServiceMocked.delete(ctxId);
        Assert.fail();
    }
}