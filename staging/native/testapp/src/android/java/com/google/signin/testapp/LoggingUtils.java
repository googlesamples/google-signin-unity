// Copyright 2018 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.signin.testapp;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A utility class, encapsulating the data and methods required to log arbitrary
 * text to the screen, via a non-editable TextView.
 */
public class LoggingUtils {
  public static TextView sTextView = null;

  public static void initLogWindow(Activity activity) {
    LinearLayout linearLayout = new LinearLayout(activity);
    ScrollView scrollView = new ScrollView(activity);
    TextView textView = new TextView(activity);
    textView.setTag("Logger");
    linearLayout.addView(scrollView);
    scrollView.addView(textView);
    Window window = activity.getWindow();
    window.takeSurface(null);
    window.setContentView(linearLayout);
    sTextView = textView;
  }

  public static void addLogText(final String text) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          if (sTextView != null) {
            sTextView.append(text);
          }
        }
      });
  }
}
