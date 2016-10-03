package fr.cnes.regards.modules.access.service;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class NavigationContextService implements INavigationContextService {

    @Override
    public NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void update(String pTinyUrl, NavigationContext pNavigationContext) throws OperationNotSupportedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(String pTinyUrl) {
        // TODO Auto-generated method stub

    }

    @Override
    public NavigationContext load(String pTinyUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NavigationContext> list() {
        // TODO Auto-generated method stub
        return null;
    }

}
