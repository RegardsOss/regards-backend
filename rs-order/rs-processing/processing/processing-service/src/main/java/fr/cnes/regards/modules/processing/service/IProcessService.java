package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;

public interface IProcessService {

    List<PProcessDTO> listAll();

    Try<Unit> setProcessProperties(String processName, Seq<String> tenants, Seq<String> datasets, Seq<String> userRoles);

}
