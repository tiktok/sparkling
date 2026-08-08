// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling;

import static org.junit.Assert.assertEquals;

import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig;
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig;
import org.junit.Test;

public class SparklingScreenOrientationJavaApiTest {
  @Test
  public void typedPoliciesAreAvailableFromJava() {
    SparklingContext sparklingContext = new SparklingContext();
    sparklingContext.setScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE);

    SparklingHybridConfig.Builder builder =
        new SparklingHybridConfig.Builder(new BaseInfoConfig(false));
    builder.setDefaultScreenOrientationPolicy(SparklingScreenOrientationPolicy.PORTRAIT);
    SparklingHybridConfig config = builder.build();

    assertEquals(
        SparklingScreenOrientationPolicy.LANDSCAPE,
        sparklingContext.getScreenOrientationPolicy());
    assertEquals(
        SparklingScreenOrientationPolicy.PORTRAIT,
        config.getDefaultScreenOrientationPolicy());
    assertEquals(
        SparklingScreenOrientationPolicy.SYSTEM,
        SparklingScreenOrientationPolicy.valueOf("SYSTEM"));
  }
}
