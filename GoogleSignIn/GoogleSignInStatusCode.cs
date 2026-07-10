// <copyright file="GoogleSignInStatusCode.cs" company="Google Inc.">
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
// </copyright>

namespace Google {

  /// <summary>
  /// Status code for the SignIn operations.
  /// </summary>
  /// <remarks>All successful status codes are less than or equal to 0.
  /// </remarks>
  public enum GoogleSignInStatusCode {
    SUCCESS_CACHE = -1,
    SUCCESS = 0,
    [System.Obsolete]
    SERVICE_MISSING = 1,
    [System.Obsolete]
    SERVICE_VERSION_UPDATE_REQUIRED = 2,
    [System.Obsolete]
    SERVICE_DISABLED = 3,
    SIGN_IN_REQUIRED = 4,
    INVALID_ACCOUNT = 5,
    RESOLUTION_REQUIRED = 6,
    NETWORK_ERROR = 7,
    INTERNAL_ERROR = 8,
    SERVICE_INVALID = 9,
    DEVELOPER_ERROR = 10,
    LICENSE_CHECK_FAILED = 11,
    ERROR = 13,
    INTERRUPTED = 14,
    TIMEOUT = 15,
    CANCELED = 16,
    API_NOT_CONNECTED = 17,
    DEAD_CLIENT = 18,
    REMOTE_EXCEPTION = 19,
    CONNECTION_SUSPENDED_DURING_CALL = 20,
    RECONNECTION_TIMED_OUT_DURING_UPDATE = 21,
    RECONNECTION_TIMED_OUT = 22
  }
}  // namespace GoogleSignIn

