package cz.jirutka.spring.data.jdbc;

import jakarta.annotation.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 * @author Olivier Rousselot
 */
public class PersistableEntityInformation<T extends Persistable<ID>, ID> extends AbstractEntityInformation<T, ID> {

    private Class<ID> idClass;

    /**
     * Creates a new {@link PersistableEntityInformation}.
     *
     * @param domainClass
     */
    @SuppressWarnings("unchecked")
    public PersistableEntityInformation(Class<T> domainClass) {

        super(domainClass);

        Class<?> idClass = ResolvableType.forClass(Persistable.class, domainClass).resolveGeneric(0);

        if (idClass == null) {
            throw new IllegalArgumentException(String.format("Could not resolve identifier type for %s!", domainClass));
        }

        this.idClass = (Class<ID>) idClass;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.AbstractEntityInformation#isNew(java.lang.Object)
     */
    @Override
    public boolean isNew(T entity) {
        return entity.isNew();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
     */
    @Nullable
    @Override
    public ID getId(T entity) {
        return entity.getId();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.EntityInformation#getIdType()
     */
    @Override
    public Class<ID> getIdType() {
        return this.idClass;
    }
}
