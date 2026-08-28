package com.example.backgammon.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbienceResumeTest {
  @Test
  fun loopResumeMsWrapsWithinTheTrack() {
    assertEquals(0, loopResumeMs(0, 60_000))
    assertEquals(12_345, loopResumeMs(12_345, 60_000))
    assertEquals(0, loopResumeMs(60_000, 60_000))
    assertEquals(5_000, loopResumeMs(65_000, 60_000))
    assertEquals(0, loopResumeMs(10_000, 0))
  }
}
