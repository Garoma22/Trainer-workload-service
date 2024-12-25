package com.trainerworkloadservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trainerworkloadservice.dto.TrainerInfoResponseDto;
import com.trainerworkloadservice.dto.TrainerWorkloadServiceDto;
import com.trainerworkloadservice.dto.YearDto;
import com.trainerworkloadservice.mapper.TrainerInfoMapper;
import com.trainerworkloadservice.model.TrainerInfo;
import com.trainerworkloadservice.model.Year;
import com.trainerworkloadservice.utils.TrainerStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TrainerInfoServiceTest {


  @Mock
  private ObjectMapper objectMapper;

  TrainerInfoMapper trainerInfoMapper;


  @InjectMocks
  private TrainerInfoService trainerInfoService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); //helps Jackson to deserialize local date
    trainerInfoService = new TrainerInfoService(objectMapper, trainerInfoMapper);
    trainerInfoService.initTestData();
  }

  @Test
  void getTrainer_shouldReturnTrainerInfo_whenUsernameExists() {

    String username = "test_trainer";


    TrainerInfo result = trainerInfoService.getTrainer(username);


    assertNotNull(result);
    assertEquals("test_trainer", result.getUsername());
    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getLastName());
    assertEquals(TrainerStatus.ACTIVE, result.getStatus());

    assertNotNull(result.getYears());
    assertFalse(result.getYears().isEmpty());


    Year year2024 = result.getYears().get(0);
    assertEquals(2024, year2024.getYear());
    assertNotNull(year2024.getMonths());
    assertEquals(1, year2024.getMonths().size());
    assertEquals(10, year2024.getMonths().get(0).getMonthDurations().get("november"));
  }

  @Test
  void getTrainer_shouldReturnNull_whenUsernameDoesNotExist() {
    String username = "non_existing_trainer";
    TrainerInfo result = trainerInfoService.getTrainer(username);
    assertNull(result);
  }

  @Test
  void isValidTrainer_shouldReturnTrue_whenUsernameIsValid() {
    TrainerInfo trainer = new TrainerInfo();
    trainer.setUsername("valid_username");
    boolean result = trainerInfoService.isValidTrainer(trainer);
    assertTrue(result);
  }

  @Test
  void isValidTrainer_shouldReturnFalse_whenUsernameIsNull() {
    TrainerInfo trainer = new TrainerInfo();
    trainer.setUsername(null);
    boolean result = trainerInfoService.isValidTrainer(trainer);
    assertFalse(result);
  }

  @Test
  void isValidTrainer_shouldReturnFalse_whenUsernameIsEmpty() {
    TrainerInfo trainer = new TrainerInfo();
    trainer.setUsername("   ");
    boolean result = trainerInfoService.isValidTrainer(trainer);
    assertFalse(result);
  }

  @Test
  void handleTraining_shouldProcessMessageSuccessfully() throws JsonProcessingException {

    String jsonMessage = """
        {
            "trainerUsername": "new_trainer",
            "trainerFirstName": "Alice",
            "trainerLastName": "Smith",
            "active": true,
            "trainingDate": "2024-11-01",
            "trainingDuration": 15
        }
        """;


    trainerInfoService.handleTraining(jsonMessage);


    TrainerInfo trainer = trainerInfoService.getTrainer("new_trainer");
    assertNotNull(trainer);
    assertEquals("Alice", trainer.getFirstName());
    assertEquals("Smith", trainer.getLastName());
    assertEquals(TrainerStatus.ACTIVE, trainer.getStatus());
  }

  @Test
  void processTrainingData_shouldAddTrainerIfNotExists() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("new_trainer");
    dto.setTrainerFirstName("Alice");
    dto.setTrainerLastName("Smith");
    dto.setActive(true);
    dto.setTrainingDate(LocalDate.of(2024, 11, 1));
    dto.setTrainingDuration(5);


    trainerInfoService.processTrainingData(dto);


    TrainerInfo trainer = trainerInfoService.getTrainer("new_trainer");
    assertThat(trainer).isNotNull();
    assertThat(trainer.getUsername()).isEqualTo("new_trainer");
    assertThat(trainer.getFirstName()).isEqualTo("Alice");
    assertThat(trainer.getLastName()).isEqualTo("Smith");
    assertThat(trainer.getStatus()).isEqualTo(TrainerStatus.ACTIVE);
    assertThat(trainer.getYears()).hasSize(1);
    assertThat(trainer.getYears().get(0).getMonths().get(0).getMonthDurations())
        .containsEntry("november", 5);
  }

  @Test
  void processTrainingData_shouldUpdateExistingTrainerMonthDuration() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("test_trainer");
    dto.setTrainingDate(LocalDate.of(2024, 11, 1));
    dto.setTrainingDuration(5);


    trainerInfoService.processTrainingData(dto);


    TrainerInfo trainer = trainerInfoService.getTrainer("test_trainer");
    assertThat(trainer).isNotNull();
    assertThat(trainer.getYears()).hasSize(1);
    assertThat(trainer.getYears().get(0).getMonths().get(0).getMonthDurations())
        .containsEntry("november", 15);
  }

  @Test
  void processTrainingData_shouldThrowExceptionWhenTrainerInvalid() {

    TrainerWorkloadServiceDto dto = new TrainerWorkloadServiceDto();
    dto.setTrainerUsername("");
    dto.setTrainingDate(LocalDate.of(2024, 11, 1));
    dto.setTrainingDuration(5);


    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      trainerInfoService.processTrainingData(dto);
    });
    assertThat(exception.getMessage()).isEqualTo("Invalid trainer data (empty username)");
  }



  @Test
  void getTrainerMonthData_shouldReturnCorrectTrainerInfoResponseDto() {

    String username = "test_trainer";


    TrainerInfoResponseDto responseDto = trainerInfoService.getTrainerMonthData(username);


    assertNotNull(responseDto);
    assertEquals("test_trainer", responseDto.getUsername());
    assertEquals("John", responseDto.getFirstName());
    assertEquals("Doe", responseDto.getLastName());
    assertEquals("ACTIVE", responseDto.getStatus());
    assertEquals(1, responseDto.getYears().size());


    YearDto yearDto = responseDto.getYears().get(0);
    assertEquals(2024, yearDto.getYear());
    assertEquals(1, yearDto.getMonths().size());
    assertEquals(10, yearDto.getMonths().get(0).get("november"));
  }

  @Test
  void getTrainerMonthData_shouldThrowExceptionWhenTrainerNotFound() {

    String nonExistingUsername = "unknown_trainer";


    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      trainerInfoService.getTrainerMonthData(nonExistingUsername);
    });
    assertEquals("Trainer not found: unknown_trainer", exception.getMessage());
  }

  @Test
  void getTrainerMonthData_shouldThrowExceptionWhenTrainerInvalid() {

    TrainerInfo invalidTrainer = new TrainerInfo();
    invalidTrainer.setUsername("");
    trainerInfoService.getTrainer("test_trainer").setUsername("");


    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      trainerInfoService.getTrainerMonthData("test_trainer");
    });
    assertEquals("Invalid trainer data (empty username)", exception.getMessage());
  }

}


