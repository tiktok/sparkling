package com.tiktok.sparkling.ota

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZephyrOtaManagerTest {

  @Test
  fun normalizeVersionUrlAddsHttpsWhenMissing() {
    assertEquals(
      "https://demo.zephyrcloud.app",
      ZephyrOtaManager.normalizeVersionUrl("demo.zephyrcloud.app/")
    )
  }

  @Test
  fun joinBundleUrlNormalizesBundlePath() {
    assertEquals(
      "https://demo.zephyrcloud.app/main.lynx.bundle",
      ZephyrOtaManager.joinBundleUrl("demo.zephyrcloud.app", "./main.lynx.bundle")
    )
  }

  @Test
  fun parseVersionInfoUsesReturnedVersionUrl() {
    val info = ZephyrOtaManager.parseVersionInfo(
      """{"version_url":"demo.zephyrcloud.app","snapshot_id":"snapshot-1"}""",
      "https://fallback.zephyrcloud.app"
    )

    assertEquals("snapshot-1", info?.snapshotId)
    assertEquals("https://demo.zephyrcloud.app", info?.versionUrl)
  }

  @Test
  fun parseVersionInfoReturnsNullWithoutSnapshotId() {
    val info = ZephyrOtaManager.parseVersionInfo(
      """{"version_url":"demo.zephyrcloud.app"}""",
      "https://fallback.zephyrcloud.app"
    )

    assertNull(info)
  }

  @Test
  fun shouldMarkUpdateReadyWhenSnapshotChanges() {
    assertTrue(ZephyrOtaManager.shouldMarkUpdateReady("snapshot-1", "snapshot-2"))
  }

  @Test
  fun shouldNotMarkUpdateReadyOnFirstSnapshotSync() {
    assertFalse(ZephyrOtaManager.shouldMarkUpdateReady(null, "snapshot-1"))
  }

  @Test
  fun shouldNotMarkUpdateReadyWhenSnapshotStaysSame() {
    assertFalse(ZephyrOtaManager.shouldMarkUpdateReady("snapshot-1", "snapshot-1"))
  }
}
