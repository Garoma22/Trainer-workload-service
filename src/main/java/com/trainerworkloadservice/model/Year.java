package com.trainerworkloadservice.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Year {

  private int year;
  private List<Month> months;


  public Year(int year) {
    this.year = year;
    this.months = new ArrayList<>();

  }

  public static Year of(int year) {
    return new Year(year);
  }
}
