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

import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.unity3d.player.UnityPlayer;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helper class used by the native C++ code to interact with Google Sign-in API.
 * The general flow is
 * Call configure, then one of signIn or signInSilently.
 */
public class GoogleSignInHelper {

  // Set to true to get more debug logging.
  public static boolean loggingEnabled = false;

  /**
   * Enables verbose logging
   */
  public static void enableDebugLogging(boolean flag) {
    loggingEnabled = flag;
  }

  private static CancellationSignal cancellationSignal;
  private static Task<AuthorizationResult> task;
  private static Function<Boolean, Task<AuthorizationResult>> signInFunction;
  public static boolean isPending() {
    return task != null && !task.isComplete() && !task.isCanceled();
  }

  public static int getStatus() {
    if(signInFunction == null)
      return CommonStatusCodes.DEVELOPER_ERROR;

    if(task == null)
      return CommonStatusCodes.SIGN_IN_REQUIRED;

    if(task.isCanceled())
      return CommonStatusCodes.CANCELED;

    if(task.isSuccessful())
      return CommonStatusCodes.SUCCESS;

    Exception e = task.getException();
    if(e != null)
    {
      logError("onFailure with INTERNAL_ERROR : " + e.getClass().toString() + " " + e.getMessage());
      return CommonStatusCodes.INTERNAL_ERROR;
    }

    return CommonStatusCodes.ERROR;
  }

  /**
   * Sets the configuration of the sign-in api that should be used.
   *
   * @param useGamesConfig     - true if the GAMES_CONFIG should be used when
   *                           signing-in.
   * @param webClientId        - the web client id of the backend server
   *                           associated with this application.
   * @param requestAuthCode    - true if a server auth code is needed. This also
   *                           requires the web
   *                           client id to be set.
   * @param forceRefreshToken  - true to force a refresh token when using the
   *                           server auth code.
   * @param requestEmail       - true if email address of the user is requested.
   * @param requestIdToken     - true if an id token for the user is requested.
   * @param hideUiPopups       - true if the popups during sign-in from the Games
   *                           API should be hidden.
   *                           This only has affect if useGamesConfig is true.
   * @param defaultAccountName - the account name to attempt to default to when
   *                           signing in.
   * @param additionalScopes   - additional API scopes to request when
   *                           authenticating.
   * @param requestHandle      - the handle to this request, created by the native
   *                           C++ code, this is used
   *                           to correlate the response with the request.
   */
  public static void configure(
          boolean useGamesConfig,
          String webClientId,
          boolean requestAuthCode,
          boolean forceRefreshToken,
          boolean requestEmail,
          boolean requestIdToken,
          boolean hideUiPopups,
          String defaultAccountName,
          String[] additionalScopes,
          IListener requestHandle) {
    logDebug("TokenFragment.configure called");

    signInFunction = new Function<Boolean, Task<AuthorizationResult>>() {
      @Override
      public Task<AuthorizationResult> apply(@NonNull Boolean silent) {
        if(isPending()) {
          TaskCompletionSource<AuthorizationResult> source = new TaskCompletionSource<>();
          source.trySetException(new Exception("Last task still pending"));
          return source.getTask();
        }

        cancellationSignal = new CancellationSignal();

        GetCredentialRequest.Builder getCredentialRequestBuilder = new GetCredentialRequest.Builder()
                .setPreferImmediatelyAvailableCredentials(hideUiPopups);

        if(silent) {
          GetGoogleIdOption.Builder getGoogleIdOptionBuilder = new GetGoogleIdOption.Builder()
                  .setFilterByAuthorizedAccounts(hideUiPopups)
                  .setAutoSelectEnabled(hideUiPopups);

          if(defaultAccountName != null)
            getGoogleIdOptionBuilder.setNonce(defaultAccountName);

          if(!Strings.isEmptyOrWhitespace(webClientId))
            getGoogleIdOptionBuilder.setServerClientId(webClientId);

          getCredentialRequestBuilder.addCredentialOption(getGoogleIdOptionBuilder.build());
        }
        else {
          GetSignInWithGoogleOption.Builder getSignInWithGoogleOptionBuilder = new GetSignInWithGoogleOption.Builder(webClientId);
          getCredentialRequestBuilder.addCredentialOption(getSignInWithGoogleOptionBuilder.build());
        }

        TaskCompletionSource<GetCredentialResponse> source = new TaskCompletionSource<>();

        CredentialManager.create(UnityPlayer.currentActivity).getCredentialAsync(UnityPlayer.currentActivity,
                getCredentialRequestBuilder.build(),
                cancellationSignal,
                TaskExecutors.MAIN_THREAD,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                  @Override
                  public void onResult(GetCredentialResponse getCredentialResponse) {
                    source.trySetResult(getCredentialResponse);
                  }

                  @Override
                  public void onError(@NotNull GetCredentialException e) {
                    source.trySetException(e);
                  }
                });

        return source.getTask().onSuccessTask(new SuccessContinuation<GetCredentialResponse, AuthorizationResult>() {
          @NonNull
          @Override
          public Task<AuthorizationResult> then(GetCredentialResponse getCredentialResponse) throws Exception {
            try {
              Credential credential = getCredentialResponse.getCredential();
              Log.i(TAG, "credential.getType() : " + credential.getType());

              GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
              requestHandle.onAuthenticated(googleIdTokenCredential);
            }
            catch (Exception e) {
              throw e;
            }

            AuthorizationRequest.Builder authorizationRequestBuilder = new AuthorizationRequest.Builder();
            if (requestAuthCode && !Strings.isEmptyOrWhitespace(webClientId))
              authorizationRequestBuilder.requestOfflineAccess(webClientId, forceRefreshToken);

            int additionalCount = additionalScopes != null ? additionalScopes.length : 0;
            List<Scope> scopes = new ArrayList<>(2 + additionalCount);
            scopes.add(new Scope(Scopes.PROFILE));
            if (requestEmail)
              scopes.add(new Scope(Scopes.EMAIL));
            if (additionalCount > 0) {
              for (String scope : additionalScopes) {
                scopes.add(new Scope(scope));
              }
            }

            if (!scopes.isEmpty())
              authorizationRequestBuilder.setRequestedScopes(scopes);

            return Identity.getAuthorizationClient(UnityPlayer.currentActivity).authorize(authorizationRequestBuilder.build());
          }
        }).addOnFailureListener(requestHandle).addOnCanceledListener(requestHandle).addOnSuccessListener(new OnSuccessListener<AuthorizationResult>() {
          @Override
          public void onSuccess(AuthorizationResult authorizationResult) {
            requestHandle.onAuthorized(authorizationResult);
          }
        }).addOnCompleteListener(new OnCompleteListener<AuthorizationResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthorizationResult> _unused) {
            cancellationSignal = null;
          }
        });
      }
    };
  }

  public static Task<AuthorizationResult> signIn() {
    task = signInFunction.apply(false);
    return task;
  }

  public static Task<AuthorizationResult> signInSilently() {
    task = signInFunction.apply(true);
    return task;
  }

  public static void cancel() {
    if(isPending() && cancellationSignal != null){
      cancellationSignal.cancel();
      cancellationSignal = null;
    }

    task = null;
  }

  public static void signOut() {
    cancel();

    CredentialManager.create(UnityPlayer.currentActivity).clearCredentialStateAsync(new ClearCredentialStateRequest(),
            new CancellationSignal(),
            TaskExecutors.MAIN_THREAD,
            new CredentialManagerCallback<Void, ClearCredentialException>() {
              @Override
              public void onResult(Void unused) {
                logInfo("signOut");
              }

              @Override
              public void onError(@NonNull ClearCredentialException e) {
                logError(e.getMessage());
              }
            });
  }

  static final String TAG = GoogleSignInHelper.class.getSimpleName();

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
}
