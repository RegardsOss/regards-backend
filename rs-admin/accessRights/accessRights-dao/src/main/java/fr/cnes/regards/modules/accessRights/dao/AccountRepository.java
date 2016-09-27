package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.Account;

@Repository
public class AccountRepository implements IAccountRepository {

    @Override
    public <S extends Account> S save(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Account> Iterable<S> save(Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account findOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Account> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Account> findAll(Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(Long pId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Account pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Iterable<? extends Account> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

}
