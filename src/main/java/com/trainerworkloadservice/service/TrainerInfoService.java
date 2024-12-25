package com.trainerworkloadservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainerworkloadservice.dto.TrainerInfoResponseDto;
import com.trainerworkloadservice.dto.TrainerWorkloadServiceDto;
import com.trainerworkloadservice.mapper.TrainerInfoMapper;
import com.trainerworkloadservice.model.Month;
import com.trainerworkloadservice.model.TrainerInfo;
import com.trainerworkloadservice.model.Year;
import com.trainerworkloadservice.utils.TrainerStatus;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.trainerworkloadservice.dto.YearDto;


@Slf4j
@Service

public class TrainerInfoService {

  private final Map<String, TrainerInfo> trainers = new HashMap<>();

  private final ObjectMapper objectMapper;

  private final TrainerInfoMapper trainerInfoMapper;

  public TrainerInfoService(ObjectMapper objectMapper, TrainerInfoMapper trainerInfoMapper) {
    this.objectMapper = objectMapper;

    this.trainerInfoMapper = trainerInfoMapper;
  }

  @PostConstruct
  public void initTestData() {
    TrainerInfo testTrainer = new TrainerInfo();
    testTrainer.setUsername("test_trainer");
    testTrainer.setFirstName("John");
    testTrainer.setLastName("Doe");
    testTrainer.setStatus(TrainerStatus.ACTIVE);

    Year year2024 = new Year(2024);
    Month month = new Month(new HashMap<>());
    month.getMonthDurations().put("november", 10);
    year2024.getMonths().add(month);
    testTrainer.getYears().add(year2024);
    trainers.put(testTrainer.getUsername(), testTrainer);

    log.info("Test data initialized: {}", testTrainer);
  }

  public TrainerInfo getTrainer(String username) {
    return trainers.get(username);
  }

  @JmsListener(destination = "trainer.workload.queue")
  public void handleTraining(String jsonMessage)
      throws JsonProcessingException {

    TrainerWorkloadServiceDto dto = objectMapper.readValue(jsonMessage,
        TrainerWorkloadServiceDto.class);

    processTrainingData(dto);
    log.info("Message processed: {}", dto);

  }

  public synchronized void processTrainingData(TrainerWorkloadServiceDto dto) {
    TrainerInfo trainer = getTrainer(dto.getTrainerUsername());

    if (trainer == null) {

      trainer = trainerInfoMapper.toTrainerInfo(dto);

      if (isValidTrainer(trainer)) {
        trainers.put(trainer.getUsername(), trainer);
      } else {
        throw new IllegalArgumentException("Invalid trainer data (empty username)");
      }
    }

    TrainerInfo finalTrainer = trainer;
    Year trainingYear = trainer.getYears().stream()
        .filter(y -> y.getYear() == dto.getTrainingDate().getYear())
        .findFirst()
        .orElseGet(() -> {
          Year newYear = new Year(dto.getTrainingDate().getYear());
          finalTrainer.getYears().add(newYear);
          return newYear;
        });

    Month trainingMonth = trainingYear.getMonths().stream()
        .findFirst()
        .orElseGet(() -> {
          Month newMonth = new Month(new HashMap<>());
          trainingYear.getMonths().add(newMonth);
          return newMonth;
        });

    String monthName = dto.getTrainingDate().getMonth().name().toLowerCase();
    trainingMonth.getMonthDurations().merge(monthName, dto.getTrainingDuration(), Integer::sum);
    log.info("Updated month data: {}", trainingMonth);
  }

  public boolean isValidTrainer(TrainerInfo trainer) {
    return trainer.getUsername() != null && !trainer.getUsername().trim().isEmpty();
  }

  public TrainerInfoResponseDto getTrainerMonthData(String trainerUsername) {
    TrainerInfo trainer = getTrainer(trainerUsername);

    if (trainer == null) {
      throw new IllegalArgumentException("Trainer not found: " + trainerUsername);
    }

    if (!isValidTrainer(trainer)) {
      throw new IllegalArgumentException("Invalid trainer data (empty username)");
    }

    List<YearDto> yearDtos = new ArrayList<>();
    for (Year y : trainer.getYears()) {
      List<Map<String, Integer>> monthList = new ArrayList<>();
      for (Month m : y.getMonths()) {
        monthList.add(m.getMonthDurations());
      }
      yearDtos.add(new YearDto(y.getYear(), monthList));
    }

    TrainerInfoResponseDto responseDto = new TrainerInfoResponseDto();
    responseDto.setUsername(trainer.getUsername());
    responseDto.setFirstName(trainer.getFirstName());
    responseDto.setLastName(trainer.getLastName());
    responseDto.setStatus(trainer.getStatus().name());
    responseDto.setYears(yearDtos);

    return responseDto;
  }
}

