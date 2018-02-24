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

#include "google_signin_user.h"  // NOLINT
#include <android/log.h>

#include "google_signin_user_impl.h"  // NOLINT
#include "jni_context.h"                 // NOLINT

#define GOOGLESIGNINACCOUNT_NAME \
  "com/google/android/gms/auth/api/signin/GoogleSignInAccount"

// String getDisplayName()
#define GETDISPLAYNAME_METHOD_NAME "getDisplayName"
#define GETDISPLAYNAME_METHOD_SIG "()Ljava/lang/String;"

// String getEmail()
#define GETEMAIL_METHOD_NAME "getEmail"
#define GETEMAIL_METHOD_SIG "()Ljava/lang/String;"

// String getFamilyName()
#define GETFAMILYNAME_METHOD_NAME "getFamilyName"
#define GETFAMILYNAME_METHOD_SIG "()Ljava/lang/String;"

// String getGivenName()
#define GETGIVENNAME_METHOD_NAME "getGivenName"
#define GETGIVENNAME_METHOD_SIG "()Ljava/lang/String;"

// String getId()
#define GETID_METHOD_NAME "getId"
#define GETID_METHOD_SIG "()Ljava/lang/String;"

// String getIdToken()
#define GETIDTOKEN_METHOD_NAME "getIdToken"
#define GETIDTOKEN_METHOD_SIG "()Ljava/lang/String;"

// String getServerAuthCode()
#define GETSERVERAUTHCODE_METHOD_NAME "getServerAuthCode"
#define GETSERVERAUTHCODE_METHOD_SIG "()Ljava/lang/String;"

// Uri getPhotoUrl()
#define GETPHOTOURL_METHOD_NAME "getPhotoUrl"
#define GETPHOTOURL_METHOD_SIG "()Landroid/net/Uri;"

#define URI_NAME "android/net/Uri"
#define TOSTRING_METHOD_NAME "toString"
#define TOSTRING_METHOD_SIG "()Ljava/lang/String;"

namespace google {
namespace signin {

jmethodID GoogleSignInUserImpl::method_getDisplayName = 0;
jmethodID GoogleSignInUserImpl::method_getEmail = 0;
jmethodID GoogleSignInUserImpl::method_getFamilyName = 0;
jmethodID GoogleSignInUserImpl::method_getGivenName = 0;
jmethodID GoogleSignInUserImpl::method_getId = 0;
jmethodID GoogleSignInUserImpl::method_getIdToken = 0;
jmethodID GoogleSignInUserImpl::method_getPhotoUrl = 0;
jmethodID GoogleSignInUserImpl::method_getServerAuthCode = 0;
jmethodID GoogleSignInUserImpl::method_uri_toString = 0;

void GoogleSignInUserImpl::Initialize(JNIContext &jni_context) {
  JNIEnv* env = jni_context.GetJniEnv();

  if (!method_getDisplayName) {
    jclass google_acct_class = jni_context.FindClass(GOOGLESIGNINACCOUNT_NAME);

    method_getDisplayName =
        env->GetMethodID(google_acct_class, GETDISPLAYNAME_METHOD_NAME,
                         GETDISPLAYNAME_METHOD_SIG);

    method_getEmail = env->GetMethodID(google_acct_class, GETEMAIL_METHOD_NAME,
                                       GETEMAIL_METHOD_SIG);

    method_getFamilyName = env->GetMethodID(
        google_acct_class, GETFAMILYNAME_METHOD_NAME, GETFAMILYNAME_METHOD_SIG);

    method_getGivenName = env->GetMethodID(
        google_acct_class, GETGIVENNAME_METHOD_NAME, GETGIVENNAME_METHOD_SIG);

    method_getId = env->GetMethodID(google_acct_class, GETID_METHOD_NAME,
                                    GETID_METHOD_SIG);

    method_getIdToken = env->GetMethodID(
        google_acct_class, GETIDTOKEN_METHOD_NAME, GETIDTOKEN_METHOD_SIG);

    method_getPhotoUrl = env->GetMethodID(
        google_acct_class, GETPHOTOURL_METHOD_NAME, GETPHOTOURL_METHOD_SIG);

    jclass uri_class = jni_context.FindClass(URI_NAME);
    method_uri_toString = env->GetMethodID(
        uri_class, TOSTRING_METHOD_NAME, TOSTRING_METHOD_SIG);

    method_getServerAuthCode =
        env->GetMethodID(google_acct_class, GETSERVERAUTHCODE_METHOD_NAME,
                         GETSERVERAUTHCODE_METHOD_SIG);
  }
}

GoogleSignInUser::GoogleSignInUser() : impl_(new GoogleSignInUserImpl()) {}
GoogleSignInUser::~GoogleSignInUser() { delete impl_; }

const char* GoogleSignInUser::GetDisplayName() const {
  return impl_->display_name.c_str();
}
const char* GoogleSignInUser::GetEmail() const { return impl_->email.c_str(); }
const char* GoogleSignInUser::GetFamilyName() const {
  return impl_->family_name.c_str();
}
const char* GoogleSignInUser::GetGivenName() const {
  return impl_->given_name.c_str();
}
const char* GoogleSignInUser::GetIdToken() const {
  return impl_->id_token.c_str();
}
const char* GoogleSignInUser::GetImageUrl() const {
  return impl_->image_url.c_str();
}
const char* GoogleSignInUser::GetServerAuthCode() const {
  return impl_->server_auth_code.c_str();
}
const char* GoogleSignInUser::GetUserId() const {
  return impl_->user_id.c_str();
}

GoogleSignInUser* GoogleSignInUserImpl::UserFromAccount(JNIContext &jni_context,
    jobject user_account) {
  if (!user_account) {
    return nullptr;
  }
  JNIEnv* env = jni_context.GetJniEnv();
  GoogleSignInUserImpl* user_impl = new GoogleSignInUserImpl();

  if (!GoogleSignInUserImpl::method_getDisplayName) {
    GoogleSignInUserImpl::Initialize(jni_context);
  }

  jstring val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getDisplayName));
  user_impl->display_name = jni_context.JStringToString(val);

  val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getEmail));
  user_impl->email = jni_context.JStringToString(val);

  val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getFamilyName));
  user_impl->family_name = jni_context.JStringToString(val);

  val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getGivenName));
  user_impl->given_name = jni_context.JStringToString(val);

  val = static_cast<jstring>(
      env->CallObjectMethod(user_account, GoogleSignInUserImpl::method_getId));
  user_impl->user_id = jni_context.JStringToString(val);

  val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getIdToken));
  user_impl->id_token = jni_context.JStringToString(val);

  jobject uri = env->CallObjectMethod(user_account,
                                      GoogleSignInUserImpl::method_getPhotoUrl);
  if (uri) {
    val = static_cast<jstring>(
        env->CallObjectMethod(uri, GoogleSignInUserImpl::method_uri_toString));
  } else {
    val = nullptr;
  }
  user_impl->image_url = jni_context.JStringToString(val);

  val = static_cast<jstring>(env->CallObjectMethod(
      user_account, GoogleSignInUserImpl::method_getServerAuthCode));
  user_impl->server_auth_code = jni_context.JStringToString(val);

  return new GoogleSignInUser(user_impl);
}

}  // namespace signin
}  // namespace google
