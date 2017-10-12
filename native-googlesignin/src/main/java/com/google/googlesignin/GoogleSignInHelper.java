/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.googlesignin;

import android.app.Activity;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Helper class used by the native C++ code to interact with Google Sign-in API. The general flow is
 * Call configure, then one of signIn or signInSilently.
 */
public class GoogleSignInHelper {

  // Set to true to get more debug logging.
  public static boolean loggingEnabled = false;
  private static final String TAG = "SignInFragment";

  /**
   * Enables verbose logging
   */
  public static void enableDebugLogging(boolean flag) {
    loggingEnabled = flag;
  }

  /**
   * Sets the configuration of the sign-in api that should be used.
   *
   * @param parentActivity - the parent activity. This API creates a fragment that is attached to
   *     this activity.
   * @param useGamesConfig - true if the GAMES_CONFIG should be used when signing-in.
   * @param webClientId - the web client id of the backend server associated with this application.
   * @param requestAuthCode - true if a server auth code is needed. This also requires the web
   *     client id to be set.
   * @param forceRefreshToken - true to force a refresh token when using the server auth code.
   * @param requestEmail - true if email address of the user is requested.
   * @param requestIdToken - true if an id token for the user is requested.
   * @param hideUiPopups - true if the popups during sign-in from the Games API should be hidden.
   *     This only has affect if useGamesConfig is true.
   * @param defaultAccountName - the account name to attempt to default to when signing in.
   * @param additionalScopes - additional API scopes to request when authenticating.
   * @param requestHandle - the handle to this request, created by the native C++ code, this is used
   *     to correlate the response with the request.
   */
  public static void configure(
      Activity parentActivity,
      boolean useGamesConfig,
      String webClientId,
      boolean requestAuthCode,
      boolean forceRefreshToken,
      boolean requestEmail,
      boolean requestIdToken,
      boolean hideUiPopups,
      String defaultAccountName,
      String[] additionalScopes,
      long requestHandle) {
    logDebug("TokenFragment.configure called");
    TokenRequest request =
        new TokenRequest(
            useGamesConfig,
            webClientId,
            requestAuthCode,
            forceRefreshToken,
            requestEmail,
            requestIdToken,
            hideUiPopups,
            defaultAccountName,
            additionalScopes,
            requestHandle);

    GoogleSignInFragment fragment = GoogleSignInFragment.getInstance(parentActivity);

    if (request.isValid()) {
      if (!fragment.submitRequest(request)) {
        logError("There is already a pending" + " authentication token request!");
      }
    } else {
      nativeOnResult(requestHandle, CommonStatusCodes.DEVELOPER_ERROR, null);
    }
  }

  public static void signIn(Activity activity, long requestHandle) {
    logDebug("AuthHelperFragment.authenticate called!");
    GoogleSignInFragment fragment = GoogleSignInFragment.getInstance(activity);

    if (!fragment.startSignIn()) {
      nativeOnResult(requestHandle, CommonStatusCodes.DEVELOPER_ERROR, null);
    }
  }

  public static void signInSilently(Activity activity, long requestHandle) {
    logDebug("AuthHelperFragment.signinSilently called!");
    GoogleSignInFragment fragment = GoogleSignInFragment.getInstance(activity);

    if (!fragment.startSignInSilently()) {
      nativeOnResult(requestHandle, CommonStatusCodes.DEVELOPER_ERROR, null);
    }
  }

  public static void signOut(Activity activity) {
    GoogleSignInFragment fragment = GoogleSignInFragment.getInstance(activity);
    fragment.signOut();
  }

  public static void disconnect(Activity activity) {
    GoogleSignInFragment fragment = GoogleSignInFragment.getInstance(activity);
    fragment.disconnect();
  }

  public static void logInfo(String msg) {
    if (loggingEnabled) {
      Log.i(TAG, msg);
    }
  }

  public static void logError(String msg) {
    Log.e(TAG, msg);
  }

  public static void logDebug(String msg) {
    if (loggingEnabled) {
      Log.d(TAG, msg);
    }
  }

  /**
   * Native callback for the authentication result.
   *
   * @param handle Identifies the request.
   * @param result Authentication result.
   * @param acct The account that is signed in, if successful.
   */
  public static native void nativeOnResult(long handle, int result, GoogleSignInAccount acct);
}
