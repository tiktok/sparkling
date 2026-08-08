// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling;

import static org.junit.Assert.assertSame;

import android.app.Application;
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, packageName = "com.tiktok.sparkling")
public class SparklingResourceFetcherJavaApiTest {
  @Test
  public void javaCanConfigurePageOverrideAndGlobalFactory() {
    SparklingResourceFetcherConfig pageConfig =
        SparklingResourceFetcherConfig.builder()
            .setGenericResourceFetcher(null)
            .setMediaResourceFetcher(null)
            .setTemplateResourceFetcher(null)
            .build();
    SparklingContext sparklingContext = new SparklingContext();
    sparklingContext.setResourceFetcherConfig(pageConfig);

    SparklingResourceFetcherFactory factory = context -> pageConfig;
    Application application = RuntimeEnvironment.getApplication();
    SparklingLynxConfig.Builder builder = new SparklingLynxConfig.Builder(application);
    builder.setResourceFetcherFactory(factory);
    SparklingLynxConfig lynxConfig = builder.build();

    assertSame(pageConfig, sparklingContext.getResourceFetcherConfig());
    assertSame(pageConfig, lynxConfig.getResourceFetcherFactory().create(sparklingContext));
  }
}
