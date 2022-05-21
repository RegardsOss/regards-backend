package fr.cnes.regards.modules.jpa.test.invalid3.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceRepository extends JpaRepository<InstanceEntity, Long> {

}
