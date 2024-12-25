package com.trainerworkloadservice.repository;

import com.trainerworkloadservice.model.TrainerInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainerInfoMongoRepository extends MongoRepository<TrainerInfo, String> {
  TrainerInfo findByUsername(String username);
}

