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

import android.accounts.Account;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Activity fragment with no UI added to the parent activity in order to manage the accessing of the
 * player's email address and tokens.
 */
public class GoogleSignInFragment extends Fragment
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  // Tag uniquely identifying this fragment.
  public static final String FRAGMENT_TAG = "signin.SignInFragment";
  private static final int FIRST_SIGN_IN_REQUEST_CODE = 9009;
  private static final int LAST_SIGN_IN_REQUEST_CODE = 0xFFFF;
  private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
  private static final List<Scope> BASIC_IDENTITY_SCOPES =
      Arrays.asList(new Scope("openid"), new Scope("profile"), new Scope("email"));

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Executor mainExecutor =
      new Executor() {
        @Override
        public void execute(Runnable command) {
          mainHandler.post(command);
        }
      };

  /**
   * Handle the Google API Client connection being connected.
   *
   * @param connectionHint - is not used.
   */
  @Override
  public void onConnected(@Nullable Bundle connectionHint) {
    GoogleSignInHelper.logDebug("onConnected!");
    TokenRequest pendingRequest = legacyClientRequest;
    if (!isCurrentRequest(pendingRequest)) {
      GoogleSignInHelper.logDebug("Ignoring connection for a stale request.");
      return;
    }
    if (legacyOperationRequest == pendingRequest) {
      return;
    }

    if (mGoogleApiClient.hasConnectedApi(Auth.GOOGLE_SIGN_IN_API)) {
      GoogleSignInHelper.logDebug("has connected auth!");
      legacySilentSignIn(pendingRequest);
    } else {
      startLegacySignInActivity(pendingRequest);
    }
  }

  @Override
  public void onConnectionSuspended(int cause) {
    GoogleSignInHelper.logDebug("onConnectionSuspended() called: " + cause);
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    GoogleSignInHelper.logError("Connection failed: " + connectionResult.getErrorCode());
    TokenRequest pendingRequest = legacyClientRequest;
    if (!isCurrentRequest(pendingRequest)) {
      GoogleSignInHelper.logDebug("Ignoring connection failure for a stale request.");
      return;
    }
    if (connectionResult.hasResolution()) {
      if (legacyOperationRequest != pendingRequest) {
        startLegacySignInActivity(pendingRequest);
      }
    } else {
      finishRequest(pendingRequest, connectionResult.getErrorCode(), null);
    }
  }

  public void disconnect() {
    final Activity activity = getActivity();
    final Account accountToRevoke = lastSignedInAccount;
    final List<Scope> scopesToRevoke = new ArrayList<>(lastGrantedScopes);
    final GoogleApiClient legacyClient = mGoogleApiClient;

    clearRequest(true);
    clearCredentialManagerState();
    lastSignedInAccount = null;
    lastGrantedScopes.clear();
    mGoogleApiClient = null;
    legacyClientRequest = null;
    legacyOperationRequest = null;

    if (accountToRevoke != null && activity != null && !scopesToRevoke.isEmpty()) {
      try {
        RevokeAccessRequest revokeRequest =
            RevokeAccessRequest.builder()
                .setAccount(accountToRevoke)
                .setScopes(scopesToRevoke)
                .build();
        Identity.getAuthorizationClient(activity)
            .revokeAccess(revokeRequest)
            .addOnCompleteListener(
                new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                      GoogleSignInHelper.logDebug("Google account access revoked.");
                    } else {
                      GoogleSignInHelper.logError(
                          "Failed to revoke Google account access: " + task.getException());
                    }
                  }
                });
      } catch (Throwable throwable) {
        GoogleSignInHelper.logError("Failed to start access revocation: " + throwable.getMessage());
      }
      if (legacyClient != null) {
        legacyClient.disconnect();
      }
    } else if (legacyClient != null) {
      try {
        Auth.GoogleSignInApi.revokeAccess(legacyClient)
            .setResultCallback(
                new ResultCallback<Status>() {
                  @Override
                  public void onResult(@NonNull Status status) {
                    if (!status.isSuccess()) {
                      GoogleSignInHelper.logError("Failed to revoke legacy access: " + status);
                    }
                    legacyClient.disconnect();
                  }
                });
      } catch (Throwable throwable) {
        GoogleSignInHelper.logError("Failed to revoke legacy access: " + throwable.getMessage());
        legacyClient.disconnect();
      }
    }
  }

  /**
   * The state of the fragment. It can only handle one sign-in request at a time, so we use these
   * values to keep track of the request lifecycle.
   */
  private enum State {
    NEW,
    READY,
    PENDING,
    PENDING_SILENT,
    BUSY
  }

  private State state = State.NEW;

  /**
   * The request to sign-in. This contains the configuration for the API client/Sign-in options and
   * the callback information used to communicate the result.
   */
  private TokenRequest request = null;

  private GoogleApiClient mGoogleApiClient;
  private CredentialManager credentialManager;
  private CancellationSignal credentialCancellationSignal;
  private TokenRequest legacyClientRequest;
  private TokenRequest legacyOperationRequest;
  private final Map<Integer, TokenRequest> legacyActivityRequests = new HashMap<>();
  private int nextSignInRequestCode = FIRST_SIGN_IN_REQUEST_CODE;
  private Account lastSignedInAccount;
  private final List<Scope> lastGrantedScopes = new ArrayList<>();

  // TODO: make config async.
  private static GoogleSignInFragment theFragment;

  /**
   * Gets the instance of the fragment.
   *
   * @param parentActivity - the activity to attach the fragment to.
   * @return the instance.
   */
  public static GoogleSignInFragment getInstance(Activity parentActivity) {
    GoogleSignInFragment fragment =
        (GoogleSignInFragment) parentActivity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);

    fragment = (fragment != null) ? fragment : theFragment;
    if (fragment == null) {
      GoogleSignInHelper.logDebug("Creating fragment");
      fragment = new GoogleSignInFragment();
      FragmentTransaction trans = parentActivity.getFragmentManager().beginTransaction();
      trans.add(fragment, FRAGMENT_TAG);
      trans.commitAllowingStateLoss();
      theFragment = fragment;
    }
    return fragment;
  }

  public synchronized boolean submitRequest(TokenRequest request) {
    if (this.request == null || this.state == State.READY) {
      this.request = request;
      return true;
    }
    GoogleSignInHelper.logError(
        String.format(
            Locale.getDefault(),
            "Existing request: %s ignoring %s.  State = %s",
            this.request,
            request,
            this.state));
    return false;
  }

  private synchronized State getState() {
    return state;
  }

  private synchronized void setState(State state) {
    this.state = state;
  }

  /**
   * Signs out and disconnects the client. NOTE: if you are using the Games API, you **MUST** call
   * Games.signout() before this method. Failure to do so will result in not being able to access
   * Games API until the application is restarted.
   */
  public void signOut() {
    clearRequest(true);
    clearCredentialManagerState();

    if (mGoogleApiClient != null) {
      try {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
      } catch (Throwable throwable) {
        GoogleSignInHelper.logError("Failed to sign out legacy client: " + throwable.getMessage());
      }
    }
  }

  /**
   * Starts the sign-in process using the Sign-in UI, if any UI is needed. This is in contrast to
   * startSignInSilently, which does not use any UI.
   *
   * @return true if the sign-in flow was started.
   */
  public boolean startSignIn() {
    if (request == null) {
      GoogleSignInHelper.logError("Request not configured! Failing authenticate");
      return false;
    }
    if (getState() == State.BUSY) {
      GoogleSignInHelper.logError("There is already a pending callback configured.");
    } else if (getState() == State.READY) {
      processRequest(false);
    } else {
      processWhenReady(false);
    }
    return true;
  }

  /**
   * Starts the sign-in silently flow.
   *
   * @return true if the flow was started successfully.
   */
  public boolean startSignInSilently() {
    if (request == null) {
      GoogleSignInHelper.logError("Request not configured! Failing authenticate");
      return false;
    }
    if (getState() == State.BUSY) {
      GoogleSignInHelper.logError("There is already a pending callback configured.");
    } else if (getState() == State.READY) {
      processRequest(true);
    } else {
      processWhenReady(true);
    }
    return true;
  }

  /**
   * Indicates that the token request has been set and it is ready to be processed. The processing
   * can start once the fragment is attached to the activity and initialized.
   *
   * @param silent - true if the sign-in should be silent.
   */
  private void processWhenReady(boolean silent) {
    GoogleSignInHelper.logInfo("Fragment not initialized yet, waiting to authenticate");
    setState(silent ? State.PENDING_SILENT : State.PENDING);
  }

  /**
   * Processes the token requests that are queued up. Credential Manager handles simple Google ID
   * token sign-ins; the legacy flow handles auth codes, extra scopes, Games, and compatibility
   * fallback.
   */
  private void processRequest(final boolean silent) {
    final TokenRequest pendingRequest = request;
    if (pendingRequest == null) {
      GoogleSignInHelper.logInfo("No pending configuration, returning");
      return;
    }

    try {
      setState(State.BUSY);

      pendingRequest
          .getPendingResponse()
          .setResultCallback(
              new ResultCallback<TokenResult>() {
                @Override
                public void onResult(@NonNull TokenResult tokenResult) {
                  GoogleSignInHelper.logDebug(
                      String.format(
                          Locale.getDefault(),
                          "Calling nativeOnResult: handle: %s, status: %d acct: %s",
                          tokenResult.getHandle(),
                          tokenResult.getStatus().getStatusCode(),
                          tokenResult.getAccount()));
                  GoogleSignInHelper.nativeOnResult(
                      tokenResult.getHandle(),
                      tokenResult.getStatus().getStatusCode(),
                      tokenResult.getAccount());
                  clearRequest(pendingRequest, false);
                }
              });

      if (startCredentialManagerRequest(pendingRequest, silent)) {
        GoogleSignInHelper.logDebug("Started Credential Manager sign-in.");
        return;
      }

      startLegacyRequest(pendingRequest, silent);
    } catch (Throwable throwable) {
      GoogleSignInHelper.logError("Exception caught! " + throwable.getMessage());
      finishRequest(pendingRequest, CommonStatusCodes.INTERNAL_ERROR, null);
    }

    GoogleSignInHelper.logDebug("Done with processRequest!");
  }

  private boolean startCredentialManagerRequest(
      final TokenRequest pendingRequest, final boolean silent) {
    final Activity activity = getActivity();
    if (activity == null
        || !isCurrentRequest(pendingRequest)
        || !pendingRequest.canUseCredentialManager()) {
      return false;
    }

    if (credentialManager == null) {
      credentialManager = CredentialManager.create(activity);
    }

    GetCredentialRequest credentialRequest =
        new GetCredentialRequest.Builder()
            .addCredentialOption(
                silent
                    ? new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(pendingRequest.getWebClientId())
                        .setAutoSelectEnabled(true)
                        .build()
                    : new GetSignInWithGoogleOption.Builder(pendingRequest.getWebClientId()).build())
            .build();

    final CancellationSignal cancellationSignal = new CancellationSignal();
    credentialCancellationSignal = cancellationSignal;
    credentialManager.getCredentialAsync(
        activity,
        credentialRequest,
        cancellationSignal,
        mainExecutor,
        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
          @Override
          public void onResult(@NonNull GetCredentialResponse result) {
            if (!isCurrentRequest(pendingRequest)) {
              GoogleSignInHelper.logDebug("Ignoring stale Credential Manager result.");
              return;
            }
            if (!handleCredentialResult(pendingRequest, result)) {
              GoogleSignInHelper.logError("Unexpected Credential Manager response, using legacy.");
              fallbackToLegacy(pendingRequest, silent, CommonStatusCodes.ERROR);
            }
          }

          @Override
          public void onError(@NonNull GetCredentialException e) {
            if (!isCurrentRequest(pendingRequest)) {
              GoogleSignInHelper.logDebug("Ignoring stale Credential Manager error.");
              return;
            }
            if (e instanceof GetCredentialCancellationException) {
              finishRequest(pendingRequest, CommonStatusCodes.CANCELED, null);
              return;
            }

            GoogleSignInHelper.logError(
                "Credential Manager failed, using legacy: " + e.getMessage());
            fallbackToLegacy(pendingRequest, silent, mapCredentialException(e));
          }
        });
    return true;
  }

  private boolean handleCredentialResult(
      TokenRequest pendingRequest, GetCredentialResponse result) {
    Credential credential = result.getCredential();
    if (!(credential instanceof CustomCredential)) {
      return false;
    }

    CustomCredential customCredential = (CustomCredential) credential;
    String credentialType = customCredential.getType();
    if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credentialType)
        && !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL.equals(credentialType)) {
      return false;
    }

    try {
      GoogleIdTokenCredential googleCredential =
          GoogleIdTokenCredential.createFrom(customCredential.getData());
      rememberSignedInAccount(
          new Account(googleCredential.getEmail(), GOOGLE_ACCOUNT_TYPE), BASIC_IDENTITY_SCOPES);
      finishRequest(
          pendingRequest,
          CommonStatusCodes.SUCCESS,
          GoogleSignInUserData.fromGoogleIdTokenCredential(googleCredential));
      return true;
    } catch (RuntimeException e) {
      GoogleSignInHelper.logError("Failed to parse Google ID token: " + e.getMessage());
      return false;
    }
  }

  private void fallbackToLegacy(
      TokenRequest pendingRequest, boolean silent, int fallbackStatus) {
    if (!isCurrentRequest(pendingRequest)) {
      return;
    }
    try {
      startLegacyRequest(pendingRequest, silent);
    } catch (Throwable throwable) {
      GoogleSignInHelper.logError("Legacy Google Sign-In fallback failed: " + throwable);
      finishRequest(pendingRequest, fallbackStatus, null);
    }
  }

  private int mapCredentialException(GetCredentialException exception) {
    if (exception instanceof GetCredentialCancellationException) {
      return CommonStatusCodes.CANCELED;
    }
    if (exception instanceof NoCredentialException) {
      return CommonStatusCodes.SIGN_IN_REQUIRED;
    }
    return CommonStatusCodes.ERROR;
  }

  private void startLegacyRequest(final TokenRequest pendingRequest, final boolean silent) {
    if (!isCurrentRequest(pendingRequest)) {
      return;
    }
    buildClient(pendingRequest);
    if (mGoogleApiClient == null) {
      return;
    }

    GoogleSignInHelper.logDebug(
        " Has connected == " + mGoogleApiClient.hasConnectedApi(Auth.GOOGLE_SIGN_IN_API));
    if (mGoogleApiClient.hasConnectedApi(Auth.GOOGLE_SIGN_IN_API)) {
      legacySilentSignIn(pendingRequest);
    } else if (!silent) {
      startLegacySignInActivity(pendingRequest);
    } else {
      legacySilentSignIn(pendingRequest);
    }
  }

  private void startLegacySignInActivity(TokenRequest pendingRequest) {
    if (!isCurrentRequest(pendingRequest) || mGoogleApiClient == null) {
      return;
    }

    legacyOperationRequest = pendingRequest;
    int requestCode = registerLegacyActivityRequest(pendingRequest);
    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(signInIntent, requestCode);
  }

  private synchronized int registerLegacyActivityRequest(TokenRequest pendingRequest) {
    int initialCode = nextSignInRequestCode;
    do {
      int requestCode = nextSignInRequestCode;
      nextSignInRequestCode =
          requestCode == LAST_SIGN_IN_REQUEST_CODE
              ? FIRST_SIGN_IN_REQUEST_CODE
              : requestCode + 1;
      if (!legacyActivityRequests.containsKey(requestCode)) {
        legacyActivityRequests.put(requestCode, pendingRequest);
        return requestCode;
      }
    } while (nextSignInRequestCode != initialCode);

    throw new IllegalStateException("No request code is available for Google Sign-In.");
  }

  @Nullable
  private synchronized TokenRequest takeLegacyActivityRequest(int requestCode) {
    return legacyActivityRequests.remove(requestCode);
  }

  private void legacySilentSignIn(final TokenRequest pendingRequest) {
    if (!isCurrentRequest(pendingRequest)) {
      return;
    }
    legacyOperationRequest = pendingRequest;
    Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient)
        .setResultCallback(
            new ResultCallback<GoogleSignInResult>() {
              @Override
              public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                if (!isCurrentRequest(pendingRequest)) {
                  GoogleSignInHelper.logDebug("Ignoring stale legacy silent result.");
                  return;
                }
                if (!googleSignInResult.isSuccess()) {
                  GoogleSignInHelper.logError(
                      "Error with silentSignIn: " + googleSignInResult.getStatus());
                }
                finishLegacyResult(pendingRequest, googleSignInResult);
              }
            });
  }

  private void finishLegacyResult(
      TokenRequest pendingRequest, @Nullable GoogleSignInResult result) {
    if (result == null) {
      finishRequest(pendingRequest, CommonStatusCodes.ERROR, null);
      return;
    }

    GoogleSignInAccount acct = result.getSignInAccount();
    if (acct != null) {
      Account account = acct.getAccount();
      if (account == null && acct.getEmail() != null) {
        account = new Account(acct.getEmail(), GOOGLE_ACCOUNT_TYPE);
      }
      rememberSignedInAccount(account, acct.getGrantedScopes());
    }
    finishRequest(
        pendingRequest,
        result.getStatus().getStatusCode(),
        GoogleSignInUserData.fromGoogleSignInAccount(acct));
  }

  private void finishRequest(
      TokenRequest pendingRequest, int status, @Nullable GoogleSignInUserData account) {
    if (!isCurrentRequest(pendingRequest)) {
      GoogleSignInHelper.logDebug("Ignoring result for a stale request.");
      return;
    }
    pendingRequest.setResult(status, account);
  }

  /**
   * Builds the Google API Client based on the configuration in the request.
   *
   * @param pendingRequest - the request for a token.
   */
  private void buildClient(TokenRequest pendingRequest) {
    if (mGoogleApiClient != null) {
      mGoogleApiClient.disconnect();
    }
    mGoogleApiClient = null;
    legacyClientRequest = pendingRequest;
    legacyOperationRequest = null;

    GoogleSignInOptions.Builder builder;

    if (pendingRequest.getUseGamesConfig()) {
      GoogleSignInHelper.logDebug("Using DEFAULT_GAMES_SIGN_IN");
      builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
    } else {
      GoogleSignInHelper.logDebug("Using DEFAULT_SIGN_IN");
      builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
    }

    if (pendingRequest.getDoAuthCode()) {
      if (!pendingRequest.getWebClientId().isEmpty()) {
        GoogleSignInHelper.logDebug(
            "Requesting AuthCode force = "
                + pendingRequest.getForceRefresh()
                + "client: "
                + pendingRequest.getWebClientId());
        builder.requestServerAuthCode(
            pendingRequest.getWebClientId(), pendingRequest.getForceRefresh());
      } else {
        GoogleSignInHelper.logError("Web client ID is needed for Auth Code");
        finishRequest(pendingRequest, CommonStatusCodes.DEVELOPER_ERROR, null);
        throw new IllegalStateException("Web client ID is needed for Auth Code");
      }
    }

    if (pendingRequest.getDoEmail()) {
      GoogleSignInHelper.logDebug("Requesting email");
      builder.requestEmail();
    }

    if (pendingRequest.getDoIdToken()) {
      if (!pendingRequest.getWebClientId().isEmpty()) {
        GoogleSignInHelper.logDebug(
            "Requesting IDToken  client: " + pendingRequest.getWebClientId());

        builder.requestIdToken(pendingRequest.getWebClientId());
      } else {
        GoogleSignInHelper.logError("Web client ID is needed for ID Token");
        finishRequest(pendingRequest, CommonStatusCodes.DEVELOPER_ERROR, null);
        throw new IllegalStateException("Web client ID is needed for ID Token");
      }
    }
    if (pendingRequest.getScopes() != null) {
      for (String s : pendingRequest.getScopes()) {
        GoogleSignInHelper.logDebug("Adding scope: " + s);

        builder.requestScopes(new Scope(s));
      }
    }

    if (pendingRequest.getHidePopups() && pendingRequest.getUseGamesConfig()) {
      GoogleSignInHelper.logDebug("hiding popup views for games API");
      builder.addExtension(getGamesExtension());
    }

    if (pendingRequest.getAccountName() != null) {
      GoogleSignInHelper.logDebug("Setting accountName: " + pendingRequest.getAccountName());

      builder.setAccountName(pendingRequest.getAccountName());
    }

    GoogleSignInOptions options = builder.build();

    GoogleApiClient.Builder clientBuilder =
        new GoogleApiClient.Builder(getActivity()).addApi(Auth.GOOGLE_SIGN_IN_API, options);
    if (pendingRequest.getUseGamesConfig()) {
      GoogleSignInHelper.logDebug("Adding games API");

      try {
        clientBuilder.addApi(getGamesAPI());
      } catch (Exception e) {
        GoogleSignInHelper.logError("Exception getting Games API: " + e.getMessage());
        legacyClientRequest = null;
        finishRequest(pendingRequest, CommonStatusCodes.DEVELOPER_ERROR, null);
        return;
      }
    }
    if (pendingRequest.getHidePopups()) {
      View invisible = new View(getActivity());
      invisible.setVisibility(View.INVISIBLE);
      invisible.setClickable(false);
      clientBuilder.setViewForPopups(invisible);
    }
    mGoogleApiClient = clientBuilder.build();
    mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
  }

  private Api<? extends Api.ApiOptions.NotRequiredOptions> getGamesAPI() {
    try {
      Class<?> gamesClass = Class.forName("com" + ".google.android.gms.games.Games");
      Field apiField = gamesClass.getField("API");
      return (Api<? extends Api.ApiOptions.NotRequiredOptions>) apiField.get(null);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Games API requested, but can't load Games class", e);
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException("Games API requested, but can't load Games API field", e);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Games API requested, but can't load Games API field", e);
    }
  }

  /**
   * Builds the games extension to hide popups using Reflection. This avoids the hard dependency on
   * Games.
   *
   * @return the extension, or throws InvalidArgumentException if games is requested, but not found.
   */
  private GoogleSignInOptionsExtension getGamesExtension() {
    try {
      Class<?> gamesClass = Class.forName("com" + ".google.android.gms.games.Games$GamesOptions");

      Method builderMethod = gamesClass.getMethod("builder");

      Object builder = builderMethod.invoke(null);

      Method setter = builder.getClass().getMethod("setShowConnectingPopup", boolean.class);

      setter.invoke(builder, false);

      Method buildMethod = builder.getClass().getMethod("build");
      return (GoogleSignInOptionsExtension) buildMethod.invoke(builder);

    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          "Games API requested, but can't load Games$GamesOptions class", e);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          "Games API requested, but can't find builder() static method.", e);
    } catch (InvocationTargetException e) {
      throw new IllegalArgumentException(
          "Games API requested, but can't invoke builder() static method.", e);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Games API requested, but can't invoke builder() static method.", e);
    }
  }

  @Override
  public void onStart() {
    super.onStart();

    if (mGoogleApiClient != null) {
      mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
    }
  }

  /**
   * Called when the fragment is visible to the user and actively running. This is generally tied to
   * {@link Activity#onResume() Activity.onResume} of the containing Activity's lifecycle.
   */
  @Override
  public void onResume() {
    GoogleSignInHelper.logDebug("onResume called");
    if (theFragment != this) {
      theFragment = this;
    }
    super.onResume();
    if (getState() == State.PENDING) {
      GoogleSignInHelper.logDebug("State is pending, calling processRequest(false)");
      processRequest(false);
    } else if (getState() == State.PENDING_SILENT) {
      GoogleSignInHelper.logDebug("State is pending_silent, calling processRequest(true)");
      processRequest(true);
    } else if (request == null) {
      GoogleSignInHelper.logDebug("State is now ready");
      setState(State.READY);
    } else {
      GoogleSignInHelper.logDebug("Keeping state while a sign-in request is active: " + getState());
    }
  }

  /**
   * Receive the result from a previous call to {@link #startActivityForResult(Intent, int)}. This
   * follows the related Activity API as described there in {@link Activity#onActivityResult(int,
   * int, Intent)}.
   *
   * @param requestCode The integer request code originally supplied to startActivityForResult(),
   *     allowing you to identify who this result came from.
   * @param resultCode The integer result code returned by the child activity through its
   *     setResult().
   * @param data An Intent, which can return result data to the caller.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    GoogleSignInHelper.logDebug("onActivityResult: " + requestCode + " " + resultCode);
    TokenRequest pendingRequest = takeLegacyActivityRequest(requestCode);
    if (pendingRequest != null) {
      if (!isCurrentRequest(pendingRequest)) {
        GoogleSignInHelper.logDebug("Ignoring stale legacy activity result.");
        return;
      }

      GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      if (result == null) {
        int returnCode = resultCode >= 0 ? CommonStatusCodes.ERROR : resultCode;
        finishRequest(pendingRequest, returnCode, null);
        GoogleSignInHelper.logError("GoogleSignIn result is null, returning error.");
      } else {
        finishLegacyResult(pendingRequest, result);
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void clearCredentialManagerState() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    if (credentialManager == null) {
      credentialManager = CredentialManager.create(activity);
    }

    credentialManager.clearCredentialStateAsync(
        new ClearCredentialStateRequest(),
        null,
        mainExecutor,
        new CredentialManagerCallback<Void, ClearCredentialException>() {
          @Override
          public void onResult(@Nullable Void result) {
            GoogleSignInHelper.logDebug("Credential Manager state cleared.");
          }

          @Override
          public void onError(@NonNull ClearCredentialException e) {
            GoogleSignInHelper.logError(
                "Failed to clear Credential Manager state: " + e.getMessage());
          }
        });
  }

  private synchronized boolean isCurrentRequest(@Nullable TokenRequest pendingRequest) {
    return pendingRequest != null && request == pendingRequest;
  }

  private synchronized void rememberSignedInAccount(
      @Nullable Account account, @Nullable Iterable<Scope> scopes) {
    lastSignedInAccount = account;
    lastGrantedScopes.clear();
    if (scopes != null) {
      for (Scope scope : scopes) {
        if (scope != null && !lastGrantedScopes.contains(scope)) {
          lastGrantedScopes.add(scope);
        }
      }
    }
    if (lastGrantedScopes.isEmpty()) {
      lastGrantedScopes.addAll(BASIC_IDENTITY_SCOPES);
    }
  }

  private void clearRequest(boolean cancel) {
    clearRequest(request, cancel);
  }

  private synchronized void clearRequest(
      @Nullable TokenRequest expectedRequest, boolean cancel) {
    if (expectedRequest == null || request != expectedRequest) {
      return;
    }

    request = null;
    setState(getActivity() != null ? State.READY : State.NEW);

    CancellationSignal cancellationSignal = credentialCancellationSignal;
    credentialCancellationSignal = null;
    if (legacyClientRequest == expectedRequest) {
      legacyClientRequest = null;
    }
    if (legacyOperationRequest == expectedRequest) {
      legacyOperationRequest = null;
    }
    for (Iterator<Map.Entry<Integer, TokenRequest>> iterator =
            legacyActivityRequests.entrySet().iterator();
        iterator.hasNext(); ) {
      if (iterator.next().getValue() == expectedRequest) {
        iterator.remove();
      }
    }

    if (cancellationSignal != null && !cancellationSignal.isCanceled()) {
      cancellationSignal.cancel();
    }
    if (cancel) {
      expectedRequest.cancel();
    }
  }
}
