package fr.timemanager.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "public_holidays")
public class PublicHolidayEntity {

  @Id
  @Column(name = "holiday_date", nullable = false, updatable = false)
  private LocalDate holidayDate;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "year", nullable = false)
  private int year;

  protected PublicHolidayEntity() {}

  public LocalDate getHolidayDate() {
    return holidayDate;
  }

  public String getLabel() {
    return label;
  }

  public int getYear() {
    return year;
  }
}
