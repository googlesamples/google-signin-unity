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

#ifndef GOOGLESIGNIN_GOOGLE_SIGNIN_USER_H
#define GOOGLESIGNIN_GOOGLE_SIGNIN_USER_H

namespace google {
namespace signin {

class GoogleSignInUserImpl;

// Represents the currently signed in user.
class GoogleSignInUser {
 public:
  ~GoogleSignInUser();

  const char* GetDisplayName() const;
  const char* GetEmail() const;
  const char* GetFamilyName() const;
  const char* GetGivenName() const;
  const char* GetIdToken() const;
  const char* GetImageUrl() const;
  const char* GetServerAuthCode() const;
  const char* GetUserId() const;

 private:
  friend class GoogleSignInUserImpl;
  GoogleSignInUser();
  GoogleSignInUser(GoogleSignInUserImpl* impl) : impl_(impl) {}
  GoogleSignInUserImpl* impl_;
};

}  // namespace signin
}  // namespace google
#endif  // GOOGLESIGNIN_GOOGLE_SIGNIN_USER_H
