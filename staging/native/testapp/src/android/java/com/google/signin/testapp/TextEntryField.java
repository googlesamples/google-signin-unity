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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

/**
 * A utility class, with a method to prompt the user to enter a line of text, and a native method to
 * sleep for a given number of milliseconds.
 */
public class TextEntryField {
  private static Object lock = new Object();
  private static String resultText = null;

  /**
   * Prompt the user with a text field, blocking until the user fills it out, then returns the text
   * they entered. If the user cancels, returns an empty string.
   */
  public static String readText(
      final Activity activity, final String title, final String message, final String placeholder) {
    resultText = null;
    // Show the alert dialog on the main thread.
    activity.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.setTitle(title);
            alertBuilder.setMessage(message);

            // Set up and add the text field.
            final EditText textField = new EditText(activity);
            textField.setHint(placeholder);
            alertBuilder.setView(textField);

            alertBuilder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {
                    synchronized (lock) {
                      resultText = textField.getText().toString();
                    }
                  }
                });

            alertBuilder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int whichButton) {
                    synchronized (lock) {
                      resultText = "";
                    }
                  }
                });

            alertBuilder.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    synchronized (lock) {
                      resultText = "";
                    }
                  }
                });
            alertBuilder.show();
          }
        });

    // In our original thread, wait for the dialog to finish, then return its result.
    while (true) {
      // Pause a second, waiting for the user to enter text.
      if (nativeSleep(1000)) {
        // If this returns true, an exit was requested.
        return "";
      }
      synchronized (lock) {
        if (resultText != null) {
          // resultText will be set to non-null when a dialog button is clicked, or the dialog
          // is canceled.
          String result = resultText;
          resultText = null; // Consume the result.
          return result;
        }
      }
    }
  }

  private static native boolean nativeSleep(int milliseconds);
}
