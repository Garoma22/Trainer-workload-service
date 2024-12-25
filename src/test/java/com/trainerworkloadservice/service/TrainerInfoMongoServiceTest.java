package com.trainerworkloadservice.service;

import com.trainerworkloadservice.dto.TrainerWorkloadServiceDto;
import com.trainerworkloadservice.mapper.TrainerInfoMapper;
import com.trainerworkloadservice.model.Month;
import com.trainerworkloadservice.model.TrainerInfo;
import com.trainerworkloadservice.model.Year;
import com.trainerworkloadservice.repository.TrainerInfoMongoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainerworkloadservice.utils.TrainerStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainerInfoMongoServiceTest {

  @Mock
  private TrainerInfoMongoRepository repository;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private TrainerInfoMapper trainerInfoMapper;

  private ActiveMQTextMessage activeMQTextMessage;



  @InjectMocks
  private TrainerInfoMongoService trainerInfoMongoService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    activeMQTextMessage = new ActiveMQTextMessage();
  }

  @Test
  void findByUsername_shouldReturnTrainerInfo_whenTrainerExists() {

    String username = "test_trainer";
    TrainerInfo mockTrainer = new TrainerInfo();
    mockTrainer.setUsername(username);
    mockTrainer.setFirstName("John");
    mockTrainer.setLastName("Doe");

    when(repository.findByUsername(username)).thenReturn(mockTrainer);


    TrainerInfo result = trainerInfoMongoService.findByUsername(username);

    assertNotNull(result);
    assertEquals(username, result.getUsername());
    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getLastName());

  }

  @Test
  void saveTrainer_shouldSaveAndReturnTrainerInfo() {

    TrainerInfo trainerInfo = new TrainerInfo();
    trainerInfo.setUsername("test_trainer");
    trainerInfo.setFirstName("John");
    trainerInfo.setLastName("Doe");

    when(repository.save(trainerInfo)).thenReturn(trainerInfo);

    TrainerInfo result = trainerInfoMongoService.saveTrainer(trainerInfo);

    assertNotNull(result);
    assertEquals("test_trainer", result.getUsername());
    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getLastName());

    verify(repository, times(1)).save(trainerInfo);
  }


  @Test
  void initializeTestData_shouldCreateTestTrainer_whenTrainerDoesNotExist() {

    String testUsername = "test_trainer";

    when(repository.findByUsername(testUsername)).thenReturn(null);

    trainerInfoMongoService.initializeTestData();

    verify(repository, times(1)).save(argThat(trainer ->
        trainer.getUsername().equals(testUsername) &&
            trainer.getFirstName().equals("John") &&
            trainer.getLastName().equals("Doe") &&
            trainer.getStatus() == TrainerStatus.ACTIVE
    ));

    verify(repository, times(1)).findByUsername(testUsername);
  }

  @Test
  void initializeTestData_shouldNotCreateTestTrainer_whenTrainerAlreadyExists() {

    String testUsername = "test_trainer";
    TrainerInfo existingTrainer = new TrainerInfo();
    existingTrainer.setUsername(testUsername);

    when(repository.findByUsername(testUsername)).thenReturn(existingTrainer);

    trainerInfoMongoService.initializeTestData();

    verify(repository, never()).save(any(TrainerInfo.class));
    verify(repository, times(1)).findByUsername(testUsername);
  }



  @Test
  void processTrainingData_shouldUpdateTrainingDuration_whenTrainerExists() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("existing_trainer");
    dto.setTrainingDate(LocalDate.of(2024, 6, 10));
    dto.setTrainingDuration(60);

    TrainerInfo existingTrainer = new TrainerInfo();
    existingTrainer.setUsername("existing_trainer");
    existingTrainer.setYears(new ArrayList<>());

    when(repository.findByUsername("existing_trainer")).thenReturn(existingTrainer);


    trainerInfoMongoService.processTrainingData(dto);


    assertEquals(1, existingTrainer.getYears().size());
    Year year = existingTrainer.getYears().get(0);
    assertEquals(2024, year.getYear());
    assertEquals(1, year.getMonths().size());

    Month month = year.getMonths().getFirst();
    assertEquals(60, month.getMonthDurations().get("june"));

    verify(repository, times(1)).save(existingTrainer);
  }


  @Test
  void processTrainingData_shouldSumTrainingDuration_whenMonthAlreadyExists() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("existing_trainer");
    dto.setTrainingDate(LocalDate.of(2024, 6, 15));
    dto.setTrainingDuration(40);

    TrainerInfo existingTrainer = new TrainerInfo();
    existingTrainer.setUsername("existing_trainer");

    Year existingYear = new Year(2024);
    Month existingMonth = new Month(new HashMap<>());
    existingMonth.getMonthDurations().put("june", 60);
    existingYear.getMonths().add(existingMonth);
    existingTrainer.getYears().add(existingYear);

    when(repository.findByUsername("existing_trainer")).thenReturn(existingTrainer);
    trainerInfoMongoService.processTrainingData(dto);

    assertEquals(1, existingTrainer.getYears().size(), "One year must be exist");
    Year year = existingTrainer.getYears().get(0);
    assertEquals(2024, year.getYear(), "Yhe year must be 2024");

    assertEquals(1, year.getMonths().size(), "One month must be exist");
    Month month = year.getMonths().get(0);

    assertEquals(100, month.getMonthDurations().get("june"), "Common time must be 100 min");
    verify(repository, times(1)).save(existingTrainer);
  }



  @Test
  void processTrainingData_shouldCreateNewTrainer_whenTrainerDoesNotExist() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("new_trainer");
    dto.setTrainingDate(LocalDate.of(2024, 6, 10));
    dto.setTrainingDuration(45);

    TrainerInfo newTrainer = new TrainerInfo();
    when(repository.findByUsername("new_trainer")).thenReturn(null);
    when(trainerInfoMapper.toTrainerInfo(dto)).thenReturn(newTrainer);

    trainerInfoMongoService.processTrainingData(dto);

    assertEquals(1, newTrainer.getYears().size());
    Year year = newTrainer.getYears().getFirst();
    assertEquals(2024, year.getYear());
    assertEquals(1, year.getMonths().size());

    Month month = year.getMonths().getFirst();
    assertEquals(45, month.getMonthDurations().get("june"));

    verify(repository, times(1)).save(newTrainer);
  }

  @Test
  void processTrainingData_shouldThrowException_whenDtoIsInvalid() {
    TrainerWorkloadServiceDto invalidDto = new TrainerWorkloadServiceDto();

    assertThrows(IllegalArgumentException.class,
        () -> trainerInfoMongoService.processTrainingData(invalidDto));
  }

  @Test
  void processTrainingData_shouldThrowException_whenDtoIsNull() {

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> trainerInfoMongoService.processTrainingData(null));

    assertEquals("Training data cannot be null.", exception.getMessage());
  }

  @Test
  void processTrainingData_shouldThrowException_whenUsernameIsNull() {
    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername(null);
    dto.setTrainingDate(LocalDate.of(2024, 6, 10));
    dto.setTrainingDuration(60);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> trainerInfoMongoService.processTrainingData(dto));
    assertEquals("Trainer username cannot be null or empty.", exception.getMessage());
  }

  @Test
  void processTrainingData_shouldThrowException_whenTrainingDateIsNull() {
    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("valid_username");
    dto.setTrainingDate(null);
    dto.setTrainingDuration(60);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> trainerInfoMongoService.processTrainingData(dto));
    assertEquals("Training date cannot be null.", exception.getMessage());
  }

  @Test
  void processTrainingData_shouldThrowException_whenTrainingDurationIsInvalid() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("valid_username");
    dto.setTrainingDate(LocalDate.of(2024, 6, 10));
    dto.setTrainingDuration(0);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> trainerInfoMongoService.processTrainingData(dto));
    assertEquals("Training duration must be greater than zero.", exception.getMessage());
  }


  @Test
  public void testHandleTraining_Success() throws Exception {

    String transactionId = "test-transaction-id";
    String jsonMessage = "{\"trainerUsername\":\"test_trainer\",\"trainingDuration\":3,\"trainingDate\":\"2023-01-01\"}";

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("test_trainer");
    dto.setTrainingDuration(3);
    dto.setTrainingDate(LocalDate.of(2023, 1, 1));

    TrainerInfo trainerInfo = new TrainerInfo();
    trainerInfo.setUsername("test_trainer");
    trainerInfo.setYears(new ArrayList<>());


    activeMQTextMessage.setText(jsonMessage);
    activeMQTextMessage.setStringProperty("transactionId", transactionId);

    when(objectMapper.readValue(jsonMessage, TrainerWorkloadServiceDto.class)).thenReturn(dto);
    when(repository.findByUsername("test_trainer")).thenReturn(trainerInfo);


    trainerInfoMongoService.handleTraining(activeMQTextMessage);


    verify(repository, times(1)).findByUsername("test_trainer");
    verify(repository, times(1)).save(trainerInfo);
  }
}
