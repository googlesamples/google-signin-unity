//
// Copyright 2017 Google Inc.
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
//

#include "google_signin_bridge.h"

#include <android/log.h>
#include <memory>

#include "google_signin.h"

// Wrapper for the GoogleSignIn object when returning it via the extern
// "C" interface.
struct GoogleSignInHolder {
  std::unique_ptr<googlesignin::GoogleSignIn> wrapped_;

  GoogleSignInHolder() : wrapped_(nullptr) {}

  GoogleSignInHolder(googlesignin::GoogleSignIn *ptr) : wrapped_(ptr) {}

  ~GoogleSignInHolder() = default;

  GoogleSignInHolder(GoogleSignInHolder const &copy) = default;

  GoogleSignInHolder(GoogleSignInHolder &&move) = delete;

  GoogleSignInHolder &operator=(GoogleSignInHolder const &copy) = delete;

  GoogleSignInHolder &operator=(GoogleSignInHolder &&move) = delete;
};

// Wrapper for the Future result from calling SignIn().
struct GoogleSignInFuture {
  std::unique_ptr<
      googlesignin::Future<googlesignin::GoogleSignIn ::SignInResult>>
      wrapped_;

  GoogleSignInFuture() : wrapped_(nullptr) {}

  GoogleSignInFuture(
      googlesignin::Future<googlesignin::GoogleSignIn ::SignInResult> *ptr)
      : wrapped_(ptr) {}

  ~GoogleSignInFuture() = default;

  GoogleSignInFuture(GoogleSignInFuture const &copy) = default;

  GoogleSignInFuture(GoogleSignInFuture &&move) = delete;

  GoogleSignInFuture &operator=(GoogleSignInFuture const &copy) = delete;

  GoogleSignInFuture &operator=(GoogleSignInFuture &&move) = delete;
};

// Wrapper for the signed in user object.
struct GoogleSignInUser {
  googlesignin::GoogleSignInUser *wrapped_;

  GoogleSignInUser() : wrapped_() {}

  GoogleSignInUser(googlesignin::GoogleSignInUser *ref) : wrapped_(ref) {
    __android_log_print(ANDROID_LOG_INFO, "native-googlesignin",
                        "In Wrapper display name is  %s",
                        ref->GetDisplayName());
  }

  ~GoogleSignInUser() = default;

  GoogleSignInUser(GoogleSignInUser const &copy) = delete;

  GoogleSignInUser(GoogleSignInUser &&move) = delete;

  GoogleSignInUser &operator=(GoogleSignInUser const &copy) = delete;

  GoogleSignInUser &operator=(GoogleSignInUser &&move) = delete;
};

GoogleSignIn_t GoogleSignIn_Create(jobject activity) {
  return new GoogleSignInHolder(new googlesignin::GoogleSignIn(activity));
}

void GoogleSignIn_Dispose(GoogleSignIn_t self) { delete self; }

void GoogleSignIn_EnableDebugLogging(GoogleSignIn_t self, bool flag) {
  self->wrapped_->EnableDebugLogging(flag);
}

void GoogleSignIn_Configure(GoogleSignIn_t self, bool useGameSignIn,
                            const char *webClientId, bool requestAuthCode,
                            bool forceTokenRefresh, bool requestEmail,
                            bool requestIdToken, bool hidePopups,
                            const char **additional_scopes, int scopes_count,
                            const char *accountName) {
  googlesignin::GoogleSignIn::Configuration configuration;

  configuration.use_game_signin = useGameSignIn;
  if (webClientId) {
    configuration.web_client_id = webClientId;
  }
  configuration.request_auth_code = requestAuthCode;
  configuration.force_token_refresh = forceTokenRefresh;
  configuration.request_email = requestEmail;
  configuration.request_id_token = requestIdToken;
  configuration.hide_ui_popups = hidePopups;
  if (accountName) {
    configuration.account_name = accountName;
  }
  if (scopes_count) {
    for(int i=0;i<scopes_count;i++) {
      configuration.additional_scopes.push_back(std::string(additional_scopes[i]));
    }
  }

  self->wrapped_->Configure(configuration);
}

GoogleSignInFuture_t GoogleSignIn_SignIn(GoogleSignIn_t self) {
  return new GoogleSignInFuture(&self->wrapped_->SignIn());
}

GoogleSignInFuture_t GoogleSignIn_SignInSilently(GoogleSignIn_t self) {
  return new GoogleSignInFuture(&self->wrapped_->SignInSilently());
}

void GoogleSignIn_Signout(GoogleSignIn_t self) { self->wrapped_->SignOut(); }

void GoogleSignIn_Disconnect(GoogleSignIn_t self) {
  self->wrapped_->Disconnect();
}

bool GoogleSignIn_Pending(GoogleSignInFuture_t self) {
  return self->wrapped_->Pending();
}

int GoogleSignIn_Status(GoogleSignInFuture_t self) {
  // Map the Android status code onto the Unity plugin enum.
  switch (self->wrapped_->Status()) {
    case googlesignin::GoogleSignIn::kStatusCodeApiNotConnected:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeApiNotConnected;
    case googlesignin::GoogleSignIn::kStatusCodeCanceled:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeCanceled;
    case googlesignin::GoogleSignIn::kStatusCodeDeveloperError:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeDeveloperError;
    case googlesignin::GoogleSignIn::kStatusCodeInternalError:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeInternalError;
    case googlesignin::GoogleSignIn::kStatusCodeInterrupted:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeInterrupted;
    case googlesignin::GoogleSignIn::kStatusCodeInvalidAccount:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeInvalidAccount;
    case googlesignin::GoogleSignIn::kStatusCodeNetworkError:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeNetworkError;
    case googlesignin::GoogleSignIn::kStatusCodeTimeout:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeInternalError;
    case googlesignin::GoogleSignIn::kStatusCodeSuccessCached:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeSuccessCached;
    case googlesignin::GoogleSignIn::kStatusCodeSuccess:
      return googlesignin::GoogleSignUnityStatusCode::kUnityStatusCodeSuccess;
    case googlesignin::GoogleSignIn::kStatusCodeError:
      // fall through to default case
    default:
      return googlesignin::GoogleSignUnityStatusCode ::kUnityStatusCodeError;
  }
}

GoogleSignInUser_t GoogleSignIn_Result(GoogleSignInFuture_t self) {
  return (self->wrapped_->Result() && self->wrapped_->Result()->User)
             ? new GoogleSignInUser(self->wrapped_->Result()->User)
             : nullptr;
}

static size_t ReturnCopiedString(const char *src, char *dest, size_t len) {
  if (dest && src && len) {
    strncpy(dest, src, len);
    return len;
  }
  return src ? strlen(src) + 1 : 0;
}

size_t GoogleSignIn_GetServerAuthCode(GoogleSignInUser_t self, char *buf,
                                      size_t len) {
  return ReturnCopiedString(self->wrapped_->GetServerAuthCode(), buf, len);
}

size_t GoogleSignIn_GetDisplayName(GoogleSignInUser_t self, char *buf,
                                   size_t len) {
  return ReturnCopiedString(self->wrapped_->GetDisplayName(), buf, len);
}

size_t GoogleSignIn_GetEmail(const GoogleSignInUser_t self, char *buf,
                             size_t len) {
  return ReturnCopiedString(self->wrapped_->GetEmail(), buf, len);
}

size_t GoogleSignIn_GetFamilyName(GoogleSignInUser_t self, char *buf,
                                  size_t len) {
  return ReturnCopiedString(self->wrapped_->GetFamilyName(), buf, len);
}

size_t GoogleSignIn_GetGivenName(GoogleSignInUser_t self, char *buf,
                                 size_t len) {
  return ReturnCopiedString(self->wrapped_->GetGivenName(), buf, len);
}

size_t GoogleSignIn_GetIdToken(GoogleSignInUser_t self, char *buf, size_t len) {
  return ReturnCopiedString(self->wrapped_->GetIdToken(), buf, len);
}

size_t GoogleSignIn_GetImageUrl(GoogleSignInUser_t self, char *buf,
                                size_t len) {
  return ReturnCopiedString(self->wrapped_->GetImageUrl(), buf, len);
}

size_t GoogleSignIn_GetUserId(GoogleSignInUser_t self, char *buf, size_t len) {
  return ReturnCopiedString(self->wrapped_->GetUserId(), buf, len);
}
