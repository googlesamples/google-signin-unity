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


// This is platform agnostic test code of the C++ GSI API.
// The platform specific bootstraps are in android/ and ios/ respectively,
// and those call into this to do the API calls.

#include "main.h"

#include "google_signin.h"
#include "future.h"

using namespace google::signin;

// Don't return until `future` is complete.
// Print a message for whether the result mathes our expectations.
// Returns true if the application should exit.
template <typename T>
bool WaitForFuture(Future<T> &future, const char* fn,
                   GoogleSignIn::StatusCode expected_error,
                   bool log_error = true) {
  // Wait for future to complete.
  LogMessage("  Calling %s...", fn);
  while (future.Pending()) {
    if (ProcessEvents(100)) return true;
  }

  // Log error result.
  if (log_error) {
    const GoogleSignIn::StatusCode error =
        static_cast<GoogleSignIn::StatusCode>(future.Status());
    if (error == expected_error) {
      LogMessage("%s completed as expected", fn);
    } else {
      LogMessage("ERROR: %s completed with error: %d", fn, error);
    }
  }
  return false;
}

extern "C" int common_main(int argc, const char* argv[]) {

    LogMessage("GSI Testapp Initialized!");

    GoogleSignIn::Configuration config = {};
    config.web_client_id = "64192632067-d37vn8fg59u8pvdgc7lc8jeve9mbbd7o.apps.googleusercontent.com"; // "YOUR_WEB_CLIENT_ID_HERE";
    config.request_id_token = true;
    config.use_game_signin = false;
    config.request_auth_code = false;

    LogMessage("Constructing...");
#if defined(__ANDROID__)
    GoogleSignIn gsi = GoogleSignIn(GetActivity(), GetJavaVM());
#else
    GoogleSignIn gsi = GoogleSignIn();
#endif  // defined(__ANDROID__)

    LogMessage("Calling Configure...");
    gsi.Configure(config);

    LogMessage("Calling SignIn...");
    Future<GoogleSignIn::SignInResult> &future = gsi.SignIn();
    WaitForFuture(future, "GoogleSignIn::SignIn()", GoogleSignIn::kStatusCodeSuccess);

    while (!ProcessEvents(1000)) {
    }

    return 0;
}
