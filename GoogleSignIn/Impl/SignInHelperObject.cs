// <copyright file="SignInHelperObject.cs" company="Google Inc.">
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
//  limitations under the License.
// </copyright>
namespace Google.Impl {
  using UnityEngine;

  ///<summary>Helper object to connect the Sign-in API to the Unity Game Scene.
  ///</summary>
  ///<remarks>This class is added to the scene so that the Google Sign-in API
  ///  can start coroutines.
  ///</remarks>
  public class SignInHelperObject : MonoBehaviour {

    private static SignInHelperObject instance;

    internal static SignInHelperObject Instance {
      get {
        if (Application.isPlaying) {
          // add an invisible game object to the scene
          GameObject obj = new GameObject("GoogleSignInHelperObject");
          DontDestroyOnLoad(obj);
          instance = obj.AddComponent<SignInHelperObject>();
        } else {
          instance = new SignInHelperObject();
        }
        return instance;
      }
    }
  }
}
