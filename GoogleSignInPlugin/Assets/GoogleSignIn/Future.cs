// <copyright file="Future.cs" company="Google Inc.">
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
  using System.Collections;
  using System.Threading.Tasks;
  using UnityEngine;

  /// <summary>
  /// Interface for implementations of the Future<T> API.
  /// </summary>
  internal interface FutureAPIImpl<T> {
    bool Pending { get; }
    GoogleSignInStatusCode Status { get; }
    T Result { get; }
  }

  /// <summary>
  /// Future return value.
  /// </summary>
  /// <remarks>This class provides a promise of a result from a method call.
  /// The typical usage is to check the Pending property until it is false.
  /// At this time either the Status or Result will be available for use.
  /// Result is only set if  the operation was successful.
  /// As a convience, a coroutine to complete a Task is provided.
  /// </remarks>
  public class Future<T> {

    private FutureAPIImpl<T> apiImpl;

    internal Future(FutureAPIImpl<T> impl) {
      apiImpl = impl;
    }

    /// <summary>
    /// Gets a value indicating whether this
    /// <see cref="T:Google.Future`1"/> is pending.
    /// </summary>
    /// <value><c>true</c> if pending; otherwise, <c>false</c>.</value>
    public bool Pending { get { return apiImpl.Pending; } }

    /// <summary>
    /// Gets the status.
    /// </summary>
    /// <value>The status is set when Pending == false.</value>
    GoogleSignInStatusCode Status { get { return apiImpl.Status; } }

    /// <summary>
    /// Gets the result.
    /// </summary>
    /// <value>The result is set when Pending == false and there is no error.
    /// </value>
    T Result { get { return apiImpl.Result; } }

    /// <summary>
    /// Waits for result then completes the TaskCompleationSource.
    /// </summary>
    /// <returns>The for result.</returns>
    /// <param name="tcs">Tcs.</param>
    internal IEnumerator WaitForResult(TaskCompletionSource<T> tcs) {
      yield return new WaitUntil(() => !Pending);
      if (Status == GoogleSignInStatusCode.Canceled) {
        tcs.SetCanceled();
      } else if (Status == GoogleSignInStatusCode.Success ||
            Status == GoogleSignInStatusCode.SuccessCached) {
        tcs.SetResult(Result);
      } else {
        tcs.SetException(new GoogleSignIn.SignInException(Status));
      }
    }
  }
}