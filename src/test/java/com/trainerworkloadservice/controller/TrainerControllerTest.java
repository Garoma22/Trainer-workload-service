package com.trainerworkloadservice.controller;

import com.trainerworkloadservice.service.TrainerInfoService;
import com.trainerworkloadservice.dto.TrainerInfoResponseDto;
import com.trainerworkloadservice.dto.YearDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainerController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainerControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TrainerInfoService trainerInfoService;

  @Test
  void getTrainerLoadingOfMonth_shouldReturnResponse() throws Exception {

    String trainerUsername = "Boris.Roris";

    YearDto yearDto = new YearDto();
    yearDto.setYear(2023);
    yearDto.setMonths(List.of(Map.of("January", 10)));

    TrainerInfoResponseDto responseDto = new TrainerInfoResponseDto();
    responseDto.setUsername("Boris.Roris");
    responseDto.setFirstName("Boris");
    responseDto.setLastName("Roris");
    responseDto.setStatus("ACTIVE");
    responseDto.setYears(List.of(yearDto));

    when(trainerInfoService.getTrainerMonthData(trainerUsername)).thenReturn(responseDto);

    mockMvc.perform(get("/trainers/{trainerUsername}/workload", trainerUsername)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("Boris.Roris"))
        .andExpect(jsonPath("$.firstName").value("Boris"))
        .andExpect(jsonPath("$.lastName").value("Roris"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.years[0].year").value(2023))
        .andExpect(jsonPath("$.years[0].months[0].January").value(10));
  }
}

