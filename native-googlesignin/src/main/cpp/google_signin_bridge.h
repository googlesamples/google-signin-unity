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
#ifndef GOOGLESIGNIN_GOOGLESIGNINBRIDGE_H
#define GOOGLESIGNIN_GOOGLESIGNINBRIDGE_H

// Definition of the extern "C" interface.  This is used
// primarily by the Unity plugin.

#include <jni.h>
#include <stddef.h>

struct GoogleSignInHolder;
typedef GoogleSignInHolder* GoogleSignIn_t;

struct GoogleSignInFuture;
typedef GoogleSignInFuture* GoogleSignInFuture_t;

struct GoogleSignInUser;
typedef GoogleSignInUser* GoogleSignInUser_t;

namespace googlesignin {
//Enum of the status codes used in the Unity plugin.  The
// Android specific codes are mapped onto this when returning status via
// GoogleSignIn_Status().
enum GoogleSignUnityStatusCode {
    kUnityStatusCodeSuccessCached = -1,
    kUnityStatusCodeSuccess = 0,
    kUnityStatusCodeApiNotConnected = 1,
    kUnityStatusCodeCanceled = 2,
    kUnityStatusCodeInterrupted = 3,
    kUnityStatusCodeInvalidAccount = 4,
    kUnityStatusCodeTimeout = 5,
    kUnityStatusCodeDeveloperError = 6,
    kUnityStatusCodeInternalError = 7,
    kUnityStatusCodeNetworkError = 8,
    kUnityStatusCodeError = 9
};
}

extern "C" {

// Create a new instance of the GoogleSignIn class.
GoogleSignIn_t GoogleSignIn_Create(jobject activity);

// Dispose the instance created by GoogleSignIn_Create().
void GoogleSignIn_Dispose(GoogleSignIn_t self);

// Enable verbose debugging
void GoogleSignIn_EnableDebugLogging(GoogleSignIn_t self, bool flag);

// Configure the sign-in process.  See GoogleSignIn::Configuration for details.
void GoogleSignIn_Configure(GoogleSignIn_t self, bool useGameSignIn,
                            const char* webClientId, bool requestAuthCode,
                            bool forceTokenRefresh, bool requestEmail,
                            bool requestIdToken, bool hidePopups,
                            const char** additional_scopes, int scopes_count,
                            const char* accountName);

// Start the sign-in process.  Returns a Future to use to get the result.
GoogleSignInFuture_t GoogleSignIn_SignIn(GoogleSignIn_t self);

// Attempts to sign in silently.  This method should be attempted first when
// when signing in "automatically".
GoogleSignInFuture_t GoogleSignIn_SignInSilently(GoogleSignIn_t self);

// Signs out. This affects the local state.
void GoogleSignIn_Signout(GoogleSignIn_t self);

// Disconnect from the app.  This revokes all tokens for the user both locally
// and on the server.
void GoogleSignIn_Disconnect(GoogleSignIn_t self);

// Accesses the Pending() method of the Future.  This avoids
// having to marshal classes and structures between C and other languages
// (i.e. C#).
bool GoogleSignIn_Pending(GoogleSignInFuture_t self);

// Accesses the Status() method of the Future.  This avoids
// having to marshal classes and structures between C and other languages
// (i.e. C#).
int GoogleSignIn_Status(GoogleSignInFuture_t self);

// Accesses the Result() method of the Future.  This avoids
// having to marshal classes and structures between C and other languages
// (i.e. C#).
GoogleSignInUser_t GoogleSignIn_Result(GoogleSignInFuture_t self);

// Accesses the AuthCode() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetServerAuthCode(GoogleSignInUser_t self, char* buf,
                                      size_t len);

// Accesses the DisplayName() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetDisplayName(GoogleSignInUser_t self, char* buf,
                                   size_t len);

// Accesses the Email() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
//
// Depending on the requested scopes and fields, this may return null.
size_t GoogleSignIn_GetEmail(GoogleSignInUser_t self, char* buf, size_t len);

// Accesses the FamilyName() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetFamilyName(GoogleSignInUser_t self, char* buf,
                                  size_t len);

// Accesses the GiveName() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetGivenName(GoogleSignInUser_t self, char* buf,
                                 size_t len);

// Accesses the IdToken() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetIdToken(GoogleSignInUser_t self, char* buf, size_t len);

// Accesses the ImageUrl() method of the GoogleSignInUser.
// This avoids having to marshal classes and structures between C and other
// languages (i.e. C#).
//
// The value is copied into buf up to len-1 characters.  If buf is null, then
// nothing is copied.  The return value is the length needed to copy the
// complete value.
size_t GoogleSignIn_GetImageUrl(GoogleSignInUser_t self, char* buf, size_t len);

size_t GoogleSignIn_GetUserId(GoogleSignInUser_t self, char* buf, size_t len);
}  // extern "C"
#endif  // GOOGLESIGNIN_GOOGLESIGNINBRIDGE_H
