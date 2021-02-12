package fr.cnes.regards.modules.authentication.dao.repository;

import fr.cnes.regards.modules.authentication.dao.entity.ServiceProviderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IServiceProviderEntityRepository extends JpaRepository<ServiceProviderEntity, Long>,
        JpaSpecificationExecutor<ServiceProviderEntity> {

    Optional<ServiceProviderEntity> findOneByName(String name);

    @Override
    default Page<ServiceProviderEntity> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<ServiceProviderEntity> serviceProviders = findAllById(idPage.getContent());
        return new PageImpl<>(serviceProviders, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select sp.id from ServiceProviderEntity sp")
    Page<Long> findIdPage(Pageable pageable);

    Long deleteByName(String name);
}
