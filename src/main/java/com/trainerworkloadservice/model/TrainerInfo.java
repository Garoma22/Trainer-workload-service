package com.trainerworkloadservice.model;

import com.trainerworkloadservice.utils.TrainerStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Document(collection = "trainer_info")
public class TrainerInfo {

  @Id
  private String id;

  @Indexed(unique = true)
  private String username;

  @Indexed
  private String firstName;

  @Indexed
  private String lastName;

  private TrainerStatus status;

  private List<Year> years = new ArrayList<>();
}
