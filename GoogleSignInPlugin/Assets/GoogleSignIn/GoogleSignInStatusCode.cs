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
    /// <summary>The operation was successful, but used the device's cache.
    /// </summary>
    SuccessCached = -1,

    /// <summary>The operation was successful.</summary>
    Success = 0,

    /// <summary>The client attempted to call a method from an API that
    /// failed to connect.</summary>
    ApiNotConnected = 1,

    /// <summary>The result was canceled either due to client disconnect
    /// or cancel().</summary>
    Canceled = 2,

    /// <summary> A blocking call was interrupted while waiting and did not
    /// run to completion.</summary>
    Interrupted = 3,

    /// <summary> The client attempted to connect to the service with an
    /// invalid account name specified. </summary>
    InvalidAccount = 4,

    /// <summary>Timed out while awaiting the result.</summary>
    Timeout = 5,

    /// <summary>The application is misconfigured.
    /// This error is not recoverable.</summary>
    /// <remarks>
    /// The developer should look at the logs after this to determine
    /// more actionable information.
    /// </remarks>
    DeveloperError = 6,

    /// <summary>An internal error occurred. Retrying should resolve the
    /// problem.</summary>
    InternalError = 7,

    /// <summary>A network error occurred. Retrying should resolve the problem.
    /// </summary>
    NetworkError = 8,

    /// <summary> The operation failed with no more detailed information.
    /// </summary>
    Error = 9,
  }
}  // namespace GoogleSignIn

