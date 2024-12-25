package com.trainerworkloadservice.mapper;

import com.trainerworkloadservice.dto.TrainerWorkloadServiceDto;
import com.trainerworkloadservice.model.TrainerInfo;
import com.trainerworkloadservice.model.Year;
import com.trainerworkloadservice.utils.TrainerStatus;
import org.mapstruct.*;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", imports = {TrainerStatus.class, LocalDate.class})
public interface TrainerInfoMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(source = "trainerUsername", target = "username")
  @Mapping(source = "trainerFirstName", target = "firstName")
  @Mapping(source = "trainerLastName", target = "lastName")
  @Mapping(source = "active", target = "status", qualifiedByName = "mapStatus")
  @Mapping(source = "trainingDate", target = "years", qualifiedByName = "mapYears")
  TrainerInfo toTrainerInfo(TrainerWorkloadServiceDto dto);

  @Named("mapStatus")
  default TrainerStatus mapStatus(boolean isActive) {
    return isActive ? TrainerStatus.ACTIVE : TrainerStatus.INACTIVE;
  }

  @Named("mapYears")
  default List<Year> mapYears(LocalDate trainingDate) {
    if (trainingDate != null) {
      return Collections.singletonList(Year.of(trainingDate.getYear()));
    }
    return Collections.emptyList();
  }
}
