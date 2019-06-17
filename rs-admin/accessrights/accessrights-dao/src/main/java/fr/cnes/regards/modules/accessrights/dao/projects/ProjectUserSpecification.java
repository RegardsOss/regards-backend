package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link ProjectUser} from repository.
 * @author Sébastien Binda
 */
public final class ProjectUserSpecification {

    private ProjectUserSpecification() {
    }

    /**
     * Filter on the given attributes and return result ordered by ascending login
     */
    public static Specification<ProjectUser> search(String status, String emailStart) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (!Strings.isNullOrEmpty(status)) {
                predicates.add(cb.equal(root.get("status"), UserStatus.valueOf(status)));
            }
            if (!Strings.isNullOrEmpty(emailStart)) {
                predicates.add(cb.like(root.get("email"), emailStart + "%"));
            }
            query.orderBy(cb.asc(root.get("email")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
