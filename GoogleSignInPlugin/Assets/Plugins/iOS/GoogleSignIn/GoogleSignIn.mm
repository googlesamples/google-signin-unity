/**
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#import "GoogleSignIn.h"
#import <GoogleSignIn/GIDGoogleUser.h>
#import <GoogleSignIn/GIDProfileData.h>
#import <GoogleSignIn/GIDSignIn.h>
#import <GoogleSignIn/GIDToken.h>
#import <GoogleSignIn/GIDSignInResult.h>

#import <memory>

// These values are in the Unity plugin code.  The iOS specific
// codes are mapped to these.
static const int kStatusCodeSuccessCached = -1;
static const int kStatusCodeSuccess = 0;
static const int kStatusCodeApiNotConnected = 1;
static const int kStatusCodeCanceled = 2;
static const int kStatusCodeInterrupted = 3;
static const int kStatusCodeInvalidAccount = 4;
static const int kStatusCodeTimeout = 5;
static const int kStatusCodeDeveloperError = 6;
static const int kStatusCodeInternalError = 7;
static const int kStatusCodeNetworkError = 8;
static const int kStatusCodeError = 9;

static NSString *cachedLoginHint = nil;
static NSArray<NSString *> *cachedAdditionalScopes = nil;

/**
 * Helper method to pause the Unity player.  This is done when showing any UI.
 */
void UnpauseUnityPlayer() {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (UnityIsPaused() > 0) {
      UnityPause(0);
    }
  });
}

// result for pending operation.  Access to this should be protected using the
// resultLock.
struct SignInResult {
  int result_code;
  bool finished;
  GIDGoogleUser *user;
  NSString *serverAuthCode;
};

std::unique_ptr<SignInResult> currentResult_;

NSRecursiveLock *resultLock = [NSRecursiveLock alloc];

/**
 * These are the external "C" methods that are imported by the Unity C# code.
 * The parameters are intended to be primative, easy to marshall.
 */
extern "C" {
/**
 * This method does nothing in the iOS implementation.  It is here
 * to make the API uniform between Android and iOS.
 */
void *GoogleSignIn_Create(void *data) { return NULL; }

void GoogleSignIn_EnableDebugLogging(void *unused, bool flag) {
  if (flag) {
    NSLog(@"GoogleSignIn: No optional logging available on iOS");
  }
}

/**
 * Configures the GIDSignIn instance.  The first parameter is unused in iOS.
 * It is here to make the API between Android and iOS uniform.
 */
bool GoogleSignIn_Configure(void *unused, bool useGameSignIn,
                            const char *webClientId, bool requestAuthCode,
                            bool forceTokenRefresh, bool requestEmail,
                            bool requestIdToken, bool hidePopups,
                            const char **additionalScopes, int scopeCount,
                            const char *accountName) {

  int scopeSize = scopeCount;

  if (scopeSize) {
    NSMutableArray *tmpary =
        [[NSMutableArray alloc] initWithCapacity:scopeSize];
    for (int i = 0; i < scopeCount; i++) {
      [tmpary addObject:[NSString stringWithUTF8String:additionalScopes[i]]];
    }

    cachedAdditionalScopes = tmpary;
  }

  if (accountName) {
    cachedLoginHint = [NSString stringWithUTF8String:accountName];
  }

  return !useGameSignIn;
}

/**
 Starts the sign-in process.  Returns and error result if error, null otherwise.
 */
static SignInResult *startSignIn() {
  bool busy = false;
  [resultLock lock];
  if (!currentResult_ || currentResult_->finished) {
    currentResult_.reset(new SignInResult());
    currentResult_->result_code = 0;
    currentResult_->finished = false;
    currentResult_->user = nil;
    currentResult_->serverAuthCode = nil;
  } else {
    busy = true;
  }
  [resultLock unlock];

  if (busy) {
    NSLog(@"ERROR: There is already a pending sign-in operation.");
    // Returned to the caller, should be deleted by calling
    // GoogleSignIn_DisposeFuture().
    return new SignInResult{.result_code = kStatusCodeDeveloperError,
                            .finished = true};
  }
  return nullptr;
}

/**
* The sign-in flow has finished and was successful if |error| is |nil|.
* Map the errors from the iOS SDK back to the Android values for consistency's
* sake in the Unity layer.
*/
void HandleSignInError(NSError * _Nullable _error) {
  NSLog(@"didSignInForUser: %@", _error.localizedDescription);
  if (currentResult_) {
    switch (_error.code) {
      case kGIDSignInErrorCodeUnknown:
        currentResult_->result_code = kStatusCodeError;
        break;
      case kGIDSignInErrorCodeKeychain:
        currentResult_->result_code = kStatusCodeInternalError;
        break;
      case kGIDSignInErrorCodeHasNoAuthInKeychain:
        currentResult_->result_code = kStatusCodeError;
        break;
      case kGIDSignInErrorCodeCanceled:
        currentResult_->result_code = kStatusCodeCanceled;
        break;
      default:
        NSLog(@"Unmapped error code: %ld, returning Error", static_cast<long>(_error.code));
        currentResult_->result_code = kStatusCodeError;
    }

    currentResult_->finished = true;
    UnpauseUnityPlayer();
  } else {
    NSLog(@"No currentResult to set status on!");
  }
}

/**
 * Sign-In.  The return value is a pointer to the currentResult object.
 */
void *GoogleSignIn_SignIn() {
  SignInResult *result = startSignIn();
  if (!result) {
    [GIDSignIn.sharedInstance signInWithPresentingViewController:UnityGetGLViewController() hint:cachedLoginHint additionalScopes:cachedAdditionalScopes completion:^(GIDSignInResult * _Nullable _result, NSError * _Nullable _error) {
      if (_error == nil) {
        if (currentResult_) {
          currentResult_->result_code = kStatusCodeSuccess;
          currentResult_->finished = true;
          currentResult_->user = _result.user;
          currentResult_->serverAuthCode = _result.serverAuthCode;
        } else {
          NSLog(@"No currentResult to set status on!");
        }
        NSLog(@"didSignInForUser: SUCCESS");
      } else {
        HandleSignInError(_error);
      }
    }];
    result = currentResult_.get();
  }
  return result;
}

/**
 * Attempt a silent sign-in. Return value is the pointer to the currentResult
 * object.
 */
void *GoogleSignIn_SignInSilently() {
  SignInResult *result = startSignIn();
  if (!result) {
    [GIDSignIn.sharedInstance restorePreviousSignInWithCompletion:^(GIDGoogleUser * _Nullable _user, NSError * _Nullable _error) {
      if (_error == nil) {
        if (currentResult_) {
          currentResult_->result_code = kStatusCodeSuccess;
          currentResult_->finished = true;
          currentResult_->user = _user;
        } else {
          NSLog(@"No currentResult to set status on!");
        }
        NSLog(@"didSignInForUser: SUCCESS");
      } else {
        HandleSignInError(_error);
      }
    }];
    result = currentResult_.get();
  }
  return result;
}

void GoogleSignIn_Signout() {
  GIDSignIn *signIn = GIDSignIn.sharedInstance;
  [signIn signOut];
}

void GoogleSignIn_Disconnect() {
  GIDSignIn *signIn = GIDSignIn.sharedInstance;
  // Finished disconnecting |user| from the app successfully if |error| is |nil|.
  [signIn disconnectWithCompletion:^(NSError *_Nullable _error) {
      if (_error == nil) {
        NSLog(@"didDisconnectWithUser: SUCCESS");
      } else {
        NSLog(@"didDisconnectWithUser: %@", _error);
      }
  }];
}

bool GoogleSignIn_Pending(SignInResult *result) {
  volatile bool ret;
  [resultLock lock];
  ret = !result->finished;
  [resultLock unlock];
  return ret;
}

SignInResult *GoogleSignIn_Result(SignInResult *result) {
  if (result && result->finished) {
    return result;
  }
  return nullptr;
}

int GoogleSignIn_Status(SignInResult *result) {
  if (result) {
    return result->result_code;
  }
  return kStatusCodeDeveloperError;
}

void GoogleSignIn_DisposeFuture(SignInResult *result) {
  if (result == currentResult_.get()) {
    currentResult_.reset(nullptr);
  } else {
    delete result;
  }
}

/**
 * Private helper function to copy NSString to char*.  If the destination is
 * non-null, the contents of src are copied up to len bytes (using strncpy). The
 * then len is returned. Otherwise returns length of the string to copy + 1.
 */
static size_t CopyNSString(NSString *src, char *dest, size_t len) {
  if (dest && src && len) {
    const char *string = [src UTF8String];
    strncpy(dest, string, len);
    return len;
  }
  return src ? src.length + 1 : 0;
}

size_t GoogleSignIn_GetServerAuthCode(SignInResult *gresult, char *buf,
                                      size_t len) {
  NSString *val = gresult->serverAuthCode;
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetDisplayName(SignInResult *gresult, char *buf,
                                   size_t len) {
  NSString *val = [gresult->user.profile name];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetEmail(SignInResult *gresult, char *buf, size_t len) {
  NSString *val = [gresult->user.profile email];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetFamilyName(SignInResult *gresult, char *buf, size_t len) {
  NSString *val = [gresult->user.profile familyName];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetGivenName(SignInResult *gresult, char *buf, size_t len) {
  NSString *val = [gresult->user.profile givenName];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetIdToken(SignInResult *gresult, char *buf, size_t len) {
  NSString *val = [gresult->user.idToken tokenString];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetImageUrl(SignInResult *gresult, char *buf, size_t len) {
  NSURL *url = [gresult->user.profile imageURLWithDimension:128];
  NSString *val = url ? [url absoluteString] : nullptr;
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetUserId(SignInResult *gresult, char *buf, size_t len) {
  NSString *val = [gresult->user userID];
  return CopyNSString(val, buf, len);
}
} // extern "C"
