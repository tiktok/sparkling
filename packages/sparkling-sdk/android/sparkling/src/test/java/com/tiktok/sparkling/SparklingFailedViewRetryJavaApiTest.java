// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.View;
import androidx.appcompat.widget.Toolbar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, packageName = "com.tiktok.sparkling")
public class SparklingFailedViewRetryJavaApiTest {
  @Test
  public void javaCanImplementRetryableErrorView() {
    Context context = RuntimeEnvironment.getApplication();
    JavaRetryableErrorView errorView = new JavaRetryableErrorView(context);
    SparklingFailedViewRetry retry = () -> false;

    errorView.setSparklingRetry(retry);

    assertSame(retry, errorView.retry);
    assertFalse(errorView.retry.retry());
    errorView.setSparklingRetry(null);
    assertNull(errorView.retry);
  }

  @Test
  public void existingJavaUiProviderNeedsNoNewMethod() {
    Context context = RuntimeEnvironment.getApplication();
    SparklingUIProvider provider =
        new SparklingUIProvider() {
          @Override
          public View getLoadingView(Context context) {
            return new View(context);
          }

          @Override
          public View getErrorView(Context context) {
            return new View(context);
          }

          @Override
          public Toolbar getToolBar(Context context) {
            return null;
          }
        };

    assertFalse(provider.getErrorView(context) instanceof SparklingRetryableErrorView);
  }

  private static final class JavaRetryableErrorView extends View
      implements SparklingRetryableErrorView {
    private SparklingFailedViewRetry retry;

    JavaRetryableErrorView(Context context) {
      super(context);
    }

    @Override
    public void setSparklingRetry(SparklingFailedViewRetry retry) {
      this.retry = retry;
    }
  }
}
