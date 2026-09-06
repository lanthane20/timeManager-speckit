package fr.timemanager.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HalfDayTest {

  @Test
  void none_noAdjustment() {
    var hd = HalfDay.NONE;
    assertThat(hd.startAdjustment()).isEqualTo(0.0);
    assertThat(hd.endAdjustment()).isEqualTo(0.0);
  }

  @Test
  void pmStart_removesHalfDay() {
    var hd = new HalfDay(HalfDayType.PM, HalfDayType.NONE);
    assertThat(hd.startAdjustment()).isEqualTo(-0.5);
    assertThat(hd.hasHalfDayStart()).isTrue();
  }

  @Test
  void amEnd_removesHalfDay() {
    var hd = new HalfDay(HalfDayType.NONE, HalfDayType.AM);
    assertThat(hd.endAdjustment()).isEqualTo(-0.5);
    assertThat(hd.hasHalfDayEnd()).isTrue();
  }
}
