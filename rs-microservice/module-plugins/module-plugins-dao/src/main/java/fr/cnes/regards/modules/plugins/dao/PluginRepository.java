/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;


import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

@Repository
public class PluginRepository implements IPluginRepository {

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(Long arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(PluginConfiguration arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Iterable<? extends PluginConfiguration> arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean exists(Long arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<PluginConfiguration> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<PluginConfiguration> findAll(Iterable<Long> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PluginConfiguration findOne(Long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends PluginConfiguration> S save(S arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends PluginConfiguration> Iterable<S> save(Iterable<S> arg0) {
        // TODO Auto-generated method stub
        return null;
    }


}
