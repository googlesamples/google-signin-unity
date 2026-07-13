/*
 * Copyright 2026 Google Inc. All Rights Reserved.
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

import android.net.Uri;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/** Data wrapper shared by the legacy GoogleSignInAccount flow and Credential Manager. */
public final class GoogleSignInUserData {
  private final String displayName;
  private final String email;
  private final String familyName;
  private final String givenName;
  private final String id;
  private final String idToken;
  private final Uri photoUrl;
  private final String serverAuthCode;

  private GoogleSignInUserData(
      String displayName,
      String email,
      String familyName,
      String givenName,
      String id,
      String idToken,
      Uri photoUrl,
      String serverAuthCode) {
    this.displayName = displayName;
    this.email = email;
    this.familyName = familyName;
    this.givenName = givenName;
    this.id = id;
    this.idToken = idToken;
    this.photoUrl = photoUrl;
    this.serverAuthCode = serverAuthCode;
  }

  @Nullable
  public static GoogleSignInUserData fromGoogleSignInAccount(
      @Nullable GoogleSignInAccount account) {
    if (account == null) {
      return null;
    }
    return new GoogleSignInUserData(
        account.getDisplayName(),
        account.getEmail(),
        account.getFamilyName(),
        account.getGivenName(),
        account.getId(),
        account.getIdToken(),
        account.getPhotoUrl(),
        account.getServerAuthCode());
  }

  @Nullable
  public static GoogleSignInUserData fromGoogleIdTokenCredential(
      @Nullable GoogleIdTokenCredential credential) {
    if (credential == null) {
      return null;
    }
    return new GoogleSignInUserData(
        credential.getDisplayName(),
        credential.getEmail(),
        credential.getFamilyName(),
        credential.getGivenName(),
        credential.getUniqueId(),
        credential.getIdToken(),
        credential.getProfilePictureUri(),
        null);
  }

  @Nullable
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  public String getEmail() {
    return email;
  }

  @Nullable
  public String getFamilyName() {
    return familyName;
  }

  @Nullable
  public String getGivenName() {
    return givenName;
  }

  @Nullable
  public String getId() {
    return id;
  }

  @Nullable
  public String getIdToken() {
    return idToken;
  }

  @Nullable
  public Uri getPhotoUrl() {
    return photoUrl;
  }

  @Nullable
  public String getServerAuthCode() {
    return serverAuthCode;
  }
}
