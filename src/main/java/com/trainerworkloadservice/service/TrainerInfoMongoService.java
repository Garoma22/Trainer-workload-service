package com.trainerworkloadservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainerworkloadservice.dto.TrainerWorkloadServiceDto;
import com.trainerworkloadservice.mapper.TrainerInfoMapper;
import com.trainerworkloadservice.model.Month;
import com.trainerworkloadservice.model.TrainerInfo;
import com.trainerworkloadservice.model.Year;
import com.trainerworkloadservice.repository.TrainerInfoMongoRepository;
import com.trainerworkloadservice.utils.TrainerStatus;
import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.Message;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@Data
public class TrainerInfoMongoService {

  private final TrainerInfoMongoRepository repository;
  private final ObjectMapper objectMapper;
  private final TrainerInfoMapper trainerInfoMapper;

  @PostConstruct
  public void init() {
    initializeTestData();
  }

  public TrainerInfo findByUsername(String username) {
    return repository.findByUsername(username);
  }

  public TrainerInfo saveTrainer(TrainerInfo trainer) {
    return repository.save(trainer);
  }

  public void initializeTestData() {
    String testUsername = "test_trainer";

    TrainerInfo existingTrainer = repository.findByUsername(testUsername);

    if (existingTrainer == null) {
      TrainerInfo trainer = new TrainerInfo();
      trainer.setUsername(testUsername);
      trainer.setFirstName("John");
      trainer.setLastName("Doe");
      trainer.setStatus(TrainerStatus.ACTIVE);

      Year year2024 = new Year(2024);
      Month month = new Month(new HashMap<>());
      month.getMonthDurations().put("november", 10);
      year2024.getMonths().add(month);

      trainer.getYears().add(year2024);

      repository.save(trainer);

      log.info("Test data has been initialized!");
    } else {
      log.info("Test data already exists. Skipping initialization.");
    }
  }

  @JmsListener(destination = "trainer.workload.queue")
  public void handleTraining(Message message) {
    try {

      String transactionId = message.getStringProperty("transactionId");

      if (message instanceof TextMessage) {
        String jsonMessage = ((TextMessage) message).getText();

        log.info("Raw message received: {}, TransactionId: {}", jsonMessage, transactionId);
        TrainerWorkloadServiceDto dto = objectMapper.readValue(jsonMessage, TrainerWorkloadServiceDto.class);

        processTrainingData(dto);
      } else {
        log.warn("Received unsupported message type: {}", message.getClass());
      }

    } catch (JMSException e) {
      log.error("Failed to extract message properties: {}", e.getMessage(), e);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JSON message: {}", e.getMessage(), e);
    }
  }

  public synchronized void processTrainingData(TrainerWorkloadServiceDto dto) {
    validateTrainerWorkloadDto(dto);
    log.info("Transaction START: Processing training data for trainer: {}",
        dto.getTrainerUsername());
    log.debug("Validation passed for input: {}", dto);

    TrainerInfo trainer = repository.findByUsername(dto.getTrainerUsername());
    if (trainer == null) {
      trainer = trainerInfoMapper.toTrainerInfo(dto);

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

    repository.save(trainer);
    log.info("Trainer data successfully updated in MongoDB: {}", trainer);
    log.info("End processing training data for username: {}", dto.getTrainerUsername());
  }


  private void validateTrainerWorkloadDto(TrainerWorkloadServiceDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("Training data cannot be null.");
    }
    if (dto.getTrainerUsername() == null || dto.getTrainerUsername().trim().isEmpty()) {
      throw new IllegalArgumentException("Trainer username cannot be null or empty.");
    }
    if (dto.getTrainingDate() == null) {
      throw new IllegalArgumentException("Training date cannot be null.");
    }
    if (dto.getTrainingDuration() <= 0) {
      throw new IllegalArgumentException("Training duration must be greater than zero.");
    }
  }
}


