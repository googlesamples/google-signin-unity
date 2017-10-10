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
#import "GoogleSignInAppController.h"
#import <GoogleSignIn/GIDAuthentication.h>
#import <GoogleSignIn/GIDGoogleUser.h>
#import <GoogleSignIn/GIDProfileData.h>
#import <GoogleSignIn/GIDSignIn.h>

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

@interface GoogleSignInAppController ()<GIDSignInUIDelegate, GIDSignInDelegate>
@end

@implementation GoogleSignInAppController

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
};

SignInResult *currentResult_ = nullptr;

NSRecursiveLock *resultLock = [NSRecursiveLock alloc];

NSString *error;
- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  [super application:application didFinishLaunchingWithOptions:launchOptions];

  // IMPORTANT: IF you are not supplying a GoogleService-Info.plist in your project that
  // contains the client id, you need to set the client id here.

  NSString *path = [[NSBundle mainBundle] pathForResource:@"GoogleService-Info" ofType:@"plist"];
  NSDictionary *dict = [NSDictionary dictionaryWithContentsOfFile:path];
  NSString *clientId = [dict objectForKey:@"CLIENT_ID"];

  // Setup the Sign-In instance.
  error = nil;
  GIDSignIn *signIn = [GIDSignIn sharedInstance];
  signIn.clientID = clientId;
  signIn.uiDelegate = self;
  signIn.delegate = self;

  return YES;
}

/**
 * Handle the auth URL.
 */
- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary *)options {
  return
      [[GIDSignIn sharedInstance] handleURL:url
                          sourceApplication:options[UIApplicationOpenURLOptionsSourceApplicationKey]
                                 annotation:options[UIApplicationOpenURLOptionsAnnotationKey]];
}

/**
 * Handle the auth URL
 */
- (BOOL)application:(UIApplication *)application
              openURL:(NSURL *)url
    sourceApplication:(NSString *)sourceApplication
           annotation:(id)annotation {
  return [[GIDSignIn sharedInstance] handleURL:url
                             sourceApplication:sourceApplication
                                    annotation:annotation];
}

/**
 * Overload the presenting of the UI so we can pause the Unity player.
 */
- (void)signIn:(GIDSignIn *)signIn presentViewController:(UIViewController *)viewController {
  UnityPause(true);
  [[self rootViewController] presentViewController:viewController animated:YES completion:nil];
}

/**
 * Overload the dismissing so we can resume the Unity player.
 */
- (void)signIn:(GIDSignIn *)signIn dismissViewController:(UIViewController *)viewController {
  UnityPause(false);
  [[self rootViewController] dismissViewControllerAnimated:YES completion:nil];
}

- (void)signInWillDispatch:(GIDSignIn *)signIn error:(NSError *)error {
  NSLog(@"signInWillDispatch: %@", error);
}

/**
 * The sign-in flow has finished and was successful if |error| is |nil|.
 * Map the errors from the iOS SDK back to the Android values for consistency's sake in the
 * Unity layer.
 */
- (void)signIn:(GIDSignIn *)signIn
    didSignInForUser:(GIDGoogleUser *)user
           withError:(NSError *)_error {
  if (_error == nil) {
    if (currentResult_) {
      currentResult_->result_code = kStatusCodeSuccess;
      currentResult_->finished = true;
    } else {
      NSLog(@"No currentResult to set status on!");
    }
    NSLog(@"didSignInForUser: SUCCESS");
  } else {
    error = _error.localizedDescription;
    NSLog(@"didSignInForUser: %@", error);
    if (currentResult_) {
      switch (_error.code) {
        case kGIDSignInErrorCodeUnknown:
          currentResult_->result_code = kStatusCodeError;
          break;
        case kGIDSignInErrorCodeKeychain:
          currentResult_->result_code = kStatusCodeInternalError;
          break;
        case kGIDSignInErrorCodeNoSignInHandlersInstalled:
          currentResult_->result_code = kStatusCodeDeveloperError;
          break;
        case kGIDSignInErrorCodeHasNoAuthInKeychain:
          currentResult_->result_code = kStatusCodeError;
          break;
        case kGIDSignInErrorCodeCanceled:
          currentResult_->result_code = kStatusCodeCanceled;
          break;
        default:
          NSLog(@"Unmapped error code: %d, returning Error", _error.code);
          currentResult_->result_code = kStatusCodeError;

      }

      currentResult_->finished = true;
      UnpauseUnityPlayer();
    } else {
      NSLog(@"No currentResult to set status on!");
    }
  }
}
// Finished disconnecting |user| from the app successfully if |error| is |nil|.
- (void)signIn:(GIDSignIn *)signIn
    didDisconnectWithUser:(GIDGoogleUser *)user
                withError:(NSError *)_error {
  if (_error == nil) {
    NSLog(@"didDisconnectWithUser: SUCCESS");
  } else {
    NSLog(@"didDisconnectWithUser: %@", _error);
    error = _error.localizedDescription;
  }
}
@end

/**
 * These are the external "C" methods that are imported by the Unity C# code.  The
 * parameters are intended to be primative, easy to marshall.
 */
extern "C" {
/**
 * This method does nothing in the iOS implementation.  It is here
 * to make the API uniform between Android and iOS.
 */
void* GoogleSignIn_Create(void* data) {
  return NULL;
}

void GoogleSignIn_EnableDebugLogging(void* unused, bool flag) {
  if (flag) NSLog(@"GoogleSignIn: No optional logging available on iOS");
}

/**
 * Configures the GIDSignIn instance.  The first parameter is unused in iOS.
 * It is here to make the API between Android and iOS uniform.
 */
bool GoogleSignIn_Configure(void* unused, bool useGameSignIn,
                                const char *webClientId, bool requestAuthCode,
                                bool forceTokenRefresh, bool requestEmail, bool requestIdToken,
                                bool hidePopups, const char **additionalScopes, int scopeCount,
                                const char *accountName) {
  if (webClientId) {
    [GIDSignIn sharedInstance].serverClientID = [NSString stringWithUTF8String:webClientId];
  }

  [GIDSignIn sharedInstance].shouldFetchBasicProfile = requestEmail;

  int scopeSize = scopeCount;

  if (scopeSize) {
    NSMutableArray *tmpary = [[NSMutableArray alloc] initWithCapacity:scopeSize];
    for (int i = 0; i < scopeCount; i++) {
      [tmpary addObject:[NSString stringWithUTF8String:additionalScopes[i]]];
    }

    [GIDSignIn sharedInstance].scopes = tmpary;
  }

  if (accountName) {
    [GIDSignIn sharedInstance].loginHint = [NSString stringWithUTF8String:accountName];
  }

  [GIDSignIn sharedInstance].shouldFetchBasicProfile = true;

  return !useGameSignIn;
}

/**
 * Sign-In.  The return value is a pointer to the currentResult object.
 */
void *GoogleSignIn_SignIn() {
  bool busy = false;
  [resultLock lock];
  if (!currentResult_ || currentResult_->finished) {
    if (currentResult_) {
      delete currentResult_;
    }
    currentResult_ = new SignInResult();
    currentResult_->result_code = 0;
    currentResult_->finished = false;
  } else {
    busy = true;
  }
  [resultLock unlock];

  if (busy) {
    NSLog(@"ERROR: There is already a pending sign-in operation.");
    return new SignInResult{.result_code = kStatusCodeDeveloperError, .finished = true};
  }

  [[GIDSignIn sharedInstance] signIn];

  return currentResult_;
}

/**
 * Attempt a silent sign-in. Return value is the pointer to the currentResult object.
 */
void *GoogleSignIn_SignInSilently() {
  bool busy = false;
  [resultLock lock];
  if (!currentResult_ || currentResult_->finished) {
    if (currentResult_) {
      delete currentResult_;
    }
    currentResult_ = new SignInResult();
    currentResult_->result_code = 0;
    currentResult_->finished = false;
  } else {
    busy = true;
  }
  [resultLock unlock];

  if (busy) {
    NSLog(@"ERROR: There is already a pending sign-in operation.");
    return new SignInResult{.result_code = kStatusCodeDeveloperError, .finished = true};
  }

  [[GIDSignIn sharedInstance] signInSilently];

  return currentResult_;
}

void GoogleSignIn_Signout() {
  error = nil;
  GIDSignIn *signIn = [GIDSignIn sharedInstance];
  [signIn signOut];
}

void GoogleSignIn_Disconnect() {
  error = nil;
  GIDSignIn *signIn = [GIDSignIn sharedInstance];
  [signIn disconnect];
}

bool GoogleSignIn_Pending(SignInResult *result) {
  bool ret = !result || !result->finished;
  NSLog(@"Pending: %d: result: %lx finished: %d", ret, (uintptr_t)result,
        result ? result->finished : false);
  return ret;
}

GIDGoogleUser *GoogleSignIn_Result(SignInResult *result) {
  if (result && result->finished) {
    GIDGoogleUser *guser = [GIDSignIn sharedInstance].currentUser;
    return guser;
  }
  return nullptr;
}

int GoogleSignIn_Status(SignInResult *result) {
  if (result) {
    return result->result_code;
  }
  return kStatusCodeDeveloperError;
}

void GoogleSignIn_DisposeFuture(SignInResult *result) { delete result; }

/**
 * Private helper function to copy NSString to char*.  If the destination is non-null,
 * the contents of src are copied up to len bytes (using strncpy). The then len is returned.
 *  Otherwise returns length of the string to copy + 1.
 */
static size_t CopyNSString(NSString *src, char *dest, size_t len) {
  if (dest && src && len) {
    const char *string = [src UTF8String];
    strncpy(dest, string, len);
    return len;
  }
  return src ? src.length + 1 : 0;
}

size_t GoogleSignIn_GetServerAuthCode(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser serverAuthCode];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetDisplayName(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser.profile name];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetEmail(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser.profile email];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetFamilyName(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser.profile familyName];
  return CopyNSString(val, buf, len);
  }

size_t GoogleSignIn_GetGivenName(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser.profile givenName];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetIdToken(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser.authentication idToken];
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetImageUrl(GIDGoogleUser *guser, char *buf, size_t len) {
  NSURL *url = [guser.profile imageURLWithDimension:128];
  NSString *val = url ? [url absoluteString] : nullptr;
  return CopyNSString(val, buf, len);
}

size_t GoogleSignIn_GetUserId(GIDGoogleUser *guser, char *buf, size_t len) {
  NSString *val = [guser userID];
  return CopyNSString(val, buf, len);
}
}  // extern "C"
