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

#ifndef GOOGLESIGNIN_GOOGLE_SIGNIN_USER_IMPL_H
#define GOOGLESIGNIN_GOOGLE_SIGNIN_USER_IMPL_H

#include <jni.h>
#include <string>

namespace googlesignin {
class GoogleSignInUserImpl {
 public:
  std::string display_name;
  std::string email;
  std::string family_name;
  std::string given_name;
  std::string id_token;
  std::string image_url;
  std::string user_id;
  std::string server_auth_code;
  static jmethodID method_getDisplayName;
  static jmethodID method_getEmail;
  static jmethodID method_getFamilyName;
  static jmethodID method_getGivenName;
  static jmethodID method_getId;
  static jmethodID method_getIdToken;
  static jmethodID method_getPhotoUrl;
  static jmethodID method_getServerAuthCode;
  static jmethodID method_uri_toString;

  static void Initialize(jobject obj);
  static GoogleSignInUser *UserFromAccount(jobject user_account);
};
}  // namespace googlesignin
#endif  // GOOGLESIGNIN_GOOGLE_SIGNIN_USER_IMPL_H
