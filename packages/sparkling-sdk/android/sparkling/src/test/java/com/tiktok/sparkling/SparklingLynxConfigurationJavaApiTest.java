// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import com.tiktok.sparkling.hybridkit.HybridKit;
import com.tiktok.sparkling.hybridkit.base.HybridKitType;
import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig;
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig;
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, packageName = "com.tiktok.sparkling")
public class SparklingLynxConfigurationJavaApiTest {
  @Test
  public void javaReceivesTypedFailureForUnsafeCombination() {
    Application application = RuntimeEnvironment.getApplication();
    HybridKit.INSTANCE.init(application);
    HybridKit.INSTANCE.setHybridConfig(
        new SparklingHybridConfig.Builder(new BaseInfoConfig(false)).build(), application);
    SparklingContext sparklingContext = new SparklingContext();
    HybridSchemeParam scheme = new HybridSchemeParam();
    scheme.setEngineType(HybridKitType.LYNX);
    scheme.setBundle("main.lynx.bundle");
    sparklingContext.setHybridSchemeParam(scheme);
    sparklingContext.setLynxViewport(new SparklingLynxViewport(320, 480));
    sparklingContext.setThreadStrategy(SparklingThreadStrategy.MULTI_THREADS);
    Sparkling sparkling = Sparkling.build(application, sparklingContext);

    SparklingLynxConfigurationException exception =
        assertThrows(
            SparklingLynxConfigurationException.class,
            () -> sparkling.createView(false));

    assertEquals(
        SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS,
        exception.getError());
    assertTrue(exception instanceof IllegalArgumentException);
  }
}
