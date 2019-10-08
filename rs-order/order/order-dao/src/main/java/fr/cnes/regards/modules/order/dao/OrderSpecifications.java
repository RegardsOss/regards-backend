package fr.cnes.regards.modules.order.dao;

import javax.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class OrderSpecifications {

    private OrderSpecifications() {
    }

    /**
     * Filter on the given attributes
     */
    public static Specification<Order> search(OrderStatus status, OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creationDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creationDate"), to));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
