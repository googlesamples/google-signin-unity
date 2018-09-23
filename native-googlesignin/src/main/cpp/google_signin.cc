// Copyright (C) 2017 Google Inc. All Rights Reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//    limitations under the License.

#include "google_signin.h"
#include <android/log.h>
#include <cassert>
#include "google_signin_user_impl.h"
#include "jni_init.h"

#define TAG "native-googlesignin"
#define HELPER_CLASSNAME "com/google/googlesignin/GoogleSignInHelper"

/*
public static void enableDebugLogging(boolean flag)
 */
#define ENABLE_DEBUG_METHOD_NAME "enableDebugLogging"
#define ENABLE_DEBUG_METHOD_SIG "(Z)V"

/*
public static void configure(Activity parentActivity,
                             boolean useGamesConfig,
                             String webClientId,
                             boolean requestAuthCode,
                             boolean forceRefreshToken,
                             boolean requestEmail,
                             boolean requestIdToken,
                             boolean hideUiPopups,
                             String defaultAccountName,
                             String[] additionalScopes,
                             long requestHandle)
*/
#define CONFIG_METHOD_NAME "configure"
#define CONFIG_METHOD_SIG   \
  "(Landroid/app/Activity;" \
  "Z"                       \
  "Ljava/lang/String;"      \
  "ZZZZZ"                   \
  "Ljava/lang/String;"      \
  "[Ljava/lang/String;"     \
  "J)V"

/*
public static void nativeSignIn()
 */
#define DISCONNECT_METHOD_NAME "disconnect"
#define DISCONNECT_METHOD_SIG "(Landroid/app/Activity;)V"

/*
public static void nativeSignIn()
 */
#define SIGNIN_METHOD_NAME "signIn"
#define SIGNIN_METHOD_SIG "(Landroid/app/Activity;J)V"

/*
public static void nativeSignIn()
 */
#define SIGNINSILENTLY_METHOD_NAME "signInSilently"
#define SIGNINSILENTLY_METHOD_SIG "(Landroid/app/Activity;J)V"

/*
public static void nativeSignOut()
 */
#define SIGNOUT_METHOD_NAME "signOut"
#define SIGNOUT_METHOD_SIG "(Landroid/app/Activity;)V"

/*
public static void nativeOnResult(long requestHandle, int result,
                                  GoogleSignInAccount acct),
 */
#define NATIVEONRESULT_METHOD_NAME "nativeOnResult"
#define NATIVEONRESULT_METHOD_SIG                                \
  "(J"                                                           \
  "I"                                                            \
  "Lcom/google/android/gms/auth/api/signin/GoogleSignInAccount;" \
  ")V"

namespace googlesignin {

class GoogleSignInFuture;

// The implementation of GoogleSignIn.  This implements the JNI interface to
// call the Java helper class the handles the authentication flow within Java.
// For the public methods see google_signin.h for details.
class GoogleSignIn::GoogleSignInImpl {
 public:
  jobject activity_;
  GoogleSignInFuture *current_result_;
  Configuration *current_configuration_;

  // Constructs the implementation providing the Java activity to use when
  // making calls.
  GoogleSignInImpl(jobject activity);
  ~GoogleSignInImpl();

  void Configure(const Configuration &configuration);

  void EnableDebugLogging(bool flag);

  // Starts the authentication process.
  Future<SignInResult> &SignIn();

  Future<SignInResult> &SignInSilently();

  // Get the result of the last sign-in.
  const Future<SignInResult> *GetLastSignInResult();

  // Signs out.
  void SignOut();

  void Disconnect();

  // Native method implementation for the Java class.
  static void NativeOnAuthResult(JNIEnv *env, jobject obj, jlong handle,
                                 jint result, jobject user);

 private:
  void CallConfigure();

  static const JNINativeMethod methods[];

  static jclass helper_clazz_;
  static jmethodID enable_debug_method_;
  static jmethodID config_method_;
  static jmethodID disconnect_method_;
  static jmethodID signin_method_;
  static jmethodID signinsilently_method_;
  static jmethodID signout_method_;
};

const JNINativeMethod GoogleSignIn::GoogleSignInImpl::methods[] = {
  {
    NATIVEONRESULT_METHOD_NAME,
    NATIVEONRESULT_METHOD_SIG,
    reinterpret_cast<void *>(
        GoogleSignIn::GoogleSignInImpl::NativeOnAuthResult),
  },
};

jclass GoogleSignIn::GoogleSignInImpl::helper_clazz_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::enable_debug_method_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::config_method_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::disconnect_method_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::signin_method_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::signinsilently_method_ = 0;
jmethodID GoogleSignIn::GoogleSignInImpl::signout_method_ = 0;

// Implementation of the SignIn future.
class GoogleSignInFuture : public Future<GoogleSignIn::SignInResult> {
  virtual int Status() const {
    return result_ ? result_->StatusCode
                   : GoogleSignIn::StatusCode::kStatusCodeUninitialized;
  }
  virtual GoogleSignIn::SignInResult *Result() const { return result_; }
  virtual bool Pending() const {
    return (!result_) || result_->StatusCode ==
                             GoogleSignIn::StatusCode::kStatusCodeUninitialized;
  }

 public:
  GoogleSignInFuture() : result_(nullptr) {}
  void SetResult(GoogleSignIn::SignInResult *result) { result_ = result; }

 private:
  GoogleSignIn::SignInResult *result_;
};

// Constructs a new instance.  The static members are initialized if need-be.
GoogleSignIn::GoogleSignInImpl::GoogleSignInImpl(jobject activity)
    : current_result_(nullptr), current_configuration_(nullptr) {
  JNIEnv *env = GetJniEnv();

  activity_ = env->NewGlobalRef(activity);

  if (!helper_clazz_) {
    // Find the java  helper class and initialize it.
    helper_clazz_ = FindClass(HELPER_CLASSNAME, activity);

    assert(helper_clazz_);

    if (helper_clazz_) {
      helper_clazz_ = (jclass)env->NewGlobalRef(helper_clazz_);
      env->RegisterNatives(helper_clazz_, methods,
                           sizeof(methods) / sizeof(methods[0]));
      enable_debug_method_ = env->GetStaticMethodID(helper_clazz_, ENABLE_DEBUG_METHOD_NAME,
                                              ENABLE_DEBUG_METHOD_SIG);
      config_method_ = env->GetStaticMethodID(helper_clazz_, CONFIG_METHOD_NAME,
                                              CONFIG_METHOD_SIG);
      disconnect_method_ = env->GetStaticMethodID(
          helper_clazz_, DISCONNECT_METHOD_NAME, DISCONNECT_METHOD_SIG);
      signin_method_ = env->GetStaticMethodID(helper_clazz_, SIGNIN_METHOD_NAME,
                                              SIGNIN_METHOD_SIG);
      signinsilently_method_ = env->GetStaticMethodID(
          helper_clazz_, SIGNINSILENTLY_METHOD_NAME, SIGNINSILENTLY_METHOD_SIG);
      signout_method_ = env->GetStaticMethodID(
          helper_clazz_, SIGNOUT_METHOD_NAME, SIGNOUT_METHOD_SIG);
    }
  }
}

GoogleSignIn::GoogleSignInImpl::~GoogleSignInImpl() {
  JNIEnv *env = GetJniEnv();

  env->DeleteGlobalRef(activity_);
  activity_ = nullptr;
  delete current_result_;
  current_result_ = nullptr;
}

void GoogleSignIn::GoogleSignInImpl::EnableDebugLogging(bool flag) {
  JNIEnv *env = GetJniEnv();

  env->CallStaticVoidMethod(helper_clazz_, enable_debug_method_, flag);

}

void GoogleSignIn::GoogleSignInImpl::Configure(
    const Configuration &configuration) {
  delete current_configuration_;
  current_configuration_ = new Configuration(configuration);

  delete current_result_;
  current_result_ = new GoogleSignInFuture();

  CallConfigure();
}

void GoogleSignIn::GoogleSignInImpl::CallConfigure() {
  JNIEnv *env = GetJniEnv();

  if (!current_configuration_) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "configuration is null!?");
    return;
  }
  jstring j_web_client_id =
      current_configuration_->web_client_id.empty() ? nullptr
          : env->NewStringUTF(current_configuration_->web_client_id.c_str());

  jstring j_account_name =
      current_configuration_->account_name.empty() ? nullptr
          : env->NewStringUTF(current_configuration_->account_name.c_str());

  jobjectArray j_auth_scopes = nullptr;

  if (current_configuration_->additional_scopes.size() > 0) {
    jclass string_clazz = FindClass("java/lang/String", activity_);
    j_auth_scopes = env->NewObjectArray(
            current_configuration_->additional_scopes.size(), string_clazz, nullptr);

    for (int i = 0; i < current_configuration_->additional_scopes.size(); i++) {
      env->SetObjectArrayElement(
          j_auth_scopes, i,
          env->NewStringUTF(current_configuration_->additional_scopes[i].c_str()));
    }
    env->DeleteLocalRef(string_clazz);
  }

  env->CallStaticVoidMethod(
      helper_clazz_, config_method_, activity_,
      current_configuration_->use_game_signin, j_web_client_id,
      current_configuration_->request_auth_code,
      current_configuration_->force_token_refresh,
      current_configuration_->request_email,
      current_configuration_->request_id_token,
      current_configuration_->hide_ui_popups, j_account_name, j_auth_scopes,
      reinterpret_cast<jlong>(current_result_));

  if (j_web_client_id) {
    env->DeleteLocalRef(j_web_client_id);
  }

  if (j_account_name) {
    env->DeleteLocalRef(j_account_name);
  }

  if (j_auth_scopes) {
    env->DeleteLocalRef(j_auth_scopes);
  }
}

Future<GoogleSignIn::SignInResult> &GoogleSignIn::GoogleSignInImpl::SignIn() {
  JNIEnv *env = GetJniEnv();

  if (current_result_) {
    current_result_->SetResult(nullptr);
  }

  //CallConfigure();

  env->CallStaticVoidMethod(helper_clazz_, signin_method_, activity_,
                            (jlong)current_result_);

  return *current_result_;
}

Future<GoogleSignIn::SignInResult>
    &GoogleSignIn::GoogleSignInImpl::SignInSilently() {
  JNIEnv *env = GetJniEnv();

  if (current_result_) {
    current_result_->SetResult(nullptr);
  }

  //CallConfigure();

  env->CallStaticVoidMethod(helper_clazz_, signinsilently_method_, activity_,
                            (jlong)current_result_);

  return *current_result_;
}

// Get the result of the last sign-in.
const Future<GoogleSignIn::SignInResult>
    *GoogleSignIn::GoogleSignInImpl::GetLastSignInResult() {
  return current_result_;
}

// Signs out.
void GoogleSignIn::GoogleSignInImpl::SignOut() {
  JNIEnv *env = GetJniEnv();

  __android_log_print(ANDROID_LOG_INFO, TAG,
                      "helper: %x method: %x activity: %x",
                      (uintptr_t)helper_clazz_, (uintptr_t)signin_method_,
                      (uintptr_t)activity_);

  env->CallStaticVoidMethod(helper_clazz_, signout_method_, activity_);
}

// Signs out.
void GoogleSignIn::GoogleSignInImpl::Disconnect() {
  JNIEnv *env = GetJniEnv();

  env->CallStaticVoidMethod(helper_clazz_, disconnect_method_, activity_);
}

void GoogleSignIn::GoogleSignInImpl::NativeOnAuthResult(
    JNIEnv *env, jobject obj, jlong handle, jint result, jobject user) {
  GoogleSignInFuture *future = reinterpret_cast<GoogleSignInFuture *>(handle);
  if (future) {
    SignInResult *rc = new GoogleSignIn::SignInResult();
    rc->StatusCode = result;
    rc->User = GoogleSignInUserImpl::UserFromAccount(user);

    if (rc->User) {
      __android_log_print(ANDROID_LOG_INFO, TAG, "User Display Name is  %s",
                          rc->User->GetDisplayName());
    }
    future->SetResult(rc);
  }
}

// Public class implementation.  These are called by external callers to use the
// Google Sign-in API.
GoogleSignIn::GoogleSignIn(jobject activity)
    : impl_(new GoogleSignInImpl(activity)) {}

void GoogleSignIn::EnableDebugLogging(bool flag) {
  impl_->EnableDebugLogging(flag);
}

void GoogleSignIn::Configure(const Configuration &configuration) {
  impl_->Configure(configuration);
}

Future<GoogleSignIn::SignInResult> &GoogleSignIn::SignIn() {
  return impl_->SignIn();
}

Future<GoogleSignIn::SignInResult> &GoogleSignIn::SignInSilently() {
  return impl_->SignInSilently();
}

const Future<GoogleSignIn::SignInResult> *GoogleSignIn::GetLastSignInResult() {
  return impl_->GetLastSignInResult();
}

void GoogleSignIn::SignOut() { impl_->SignOut(); }

void GoogleSignIn::Disconnect() { impl_->Disconnect(); }

}  // namespace googlesignin
