// <copyright file="GoogleSignInImpl.cs" company="Google Inc.">
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

namespace Google.Impl {
  using System;
  using System.Collections.Generic;
  using System.Runtime.InteropServices;

  internal class GoogleSignInImpl : BaseObject, ISignInImpl {

#if UNITY_ANDROID
    private const string DllName = "native-googlesignin";
#else
    private const string DllName = "__Internal";
#endif

    internal GoogleSignInImpl(GoogleSignInConfiguration configuration)
          : base(GoogleSignIn_Create(GetPlayerActivity())) {

      if (configuration != null) {
        List<string> scopes = new List<string>();
        if (configuration.AdditionalScopes != null) {
          scopes.AddRange(configuration.AdditionalScopes);
        }
        GoogleSignIn_Configure(SelfPtr(), configuration.UseGameSignIn,
                     configuration.WebClientId,
                     configuration.RequestAuthCode,
                     configuration.ForceTokenRefresh,
                     configuration.RequestEmail,
                     configuration.RequestIdToken,
                     configuration.HidePopups,
                     scopes.ToArray(),
                     scopes.Count,
                     configuration.AccountName);
      }
    }

    /// <summary>Enables/Disables verbose logging to help troubleshooting</summary>
    public void EnableDebugLogging(bool flag) {
        GoogleSignIn_EnableDebugLogging(SelfPtr(), flag);
    }

    /// <summary>
    /// Starts the authentication process.
    /// </summary>
    /// <remarks>
    /// The authenication process is started and may display account picker
    /// popups and consent prompts based on the state of authentication and
    /// the requested elements.
    /// </remarks>
    public Future<GoogleSignInUser> SignIn() {
      IntPtr nativeFuture = GoogleSignIn_SignIn(SelfPtr());
      return new Future<GoogleSignInUser>(new NativeFuture(nativeFuture));
    }

    /// <summary>
    /// Starts the authentication process.
    /// </summary>
    /// <remarks>
    /// The authenication process is started and may display account picker
    /// popups and consent prompts based on the state of authentication and
    /// the requested elements.
    /// </remarks>
    public Future<GoogleSignInUser> SignInSilently() {
      IntPtr nativeFuture = GoogleSignIn_SignInSilently(SelfPtr());
      return new Future<GoogleSignInUser>(new NativeFuture(nativeFuture));
    }

    /// <summary>
    /// Signs out the User.
    /// </summary>
    public void SignOut() {
      GoogleSignIn_Signout(SelfPtr());
    }

    /// <summary>
    /// Disconnects the user from the application and revokes all consent.
    /// </summary>
    public void Disconnect() {
      GoogleSignIn_Disconnect(SelfPtr());
    }

    /// <summary>
    /// Creates an instance of the native Google Sign-In implementation.
    /// </summary>
    /// <remarks>
    ///  For Android this must be the JNI raw object for the parentActivity.
    ///  For iOS it is ignored.
    /// </remarks>
    /// <returns>The pointer to the instance.</returns>
    /// <param name="data">Data used in creating the instance.</param>
    [DllImport(DllName)]
    static extern IntPtr GoogleSignIn_Create(IntPtr data);

    [DllImport(DllName)]
    static extern void GoogleSignIn_EnableDebugLogging(HandleRef self, bool flag);

    [DllImport(DllName)]
    static extern bool GoogleSignIn_Configure(HandleRef self,
      bool useGameSignIn, string webClientId,
      bool requestAuthCode, bool forceTokenRefresh, bool requestEmail,
      bool requestIdToken, bool hidePopups, string[] additionalScopes,
      int scopeCount, string accountName);

    [DllImport(DllName)]
    static extern IntPtr GoogleSignIn_SignIn(HandleRef self);

    [DllImport(DllName)]
    static extern IntPtr GoogleSignIn_SignInSilently(HandleRef self);

    [DllImport(DllName)]
    static extern void GoogleSignIn_Signout(HandleRef self);

    [DllImport(DllName)]
    static extern void GoogleSignIn_Disconnect(HandleRef self);

    [DllImport(DllName)]
    internal static extern void GoogleSignIn_DisposeFuture(HandleRef self);

    [DllImport(DllName)]
    internal static extern bool GoogleSignIn_Pending(HandleRef self);

    [DllImport(DllName)]
    internal static extern IntPtr GoogleSignIn_Result(HandleRef self);

    [DllImport(DllName)]
    internal static extern int GoogleSignIn_Status(HandleRef self);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetServerAuthCode(
      HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetDisplayName(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetEmail(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetFamilyName(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetGivenName(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetIdToken(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetImageUrl(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    [DllImport(DllName)]
    internal static extern UIntPtr GoogleSignIn_GetUserId(HandleRef self,
      [In, Out] byte[] bytes, UIntPtr len);

    // Gets the Unity player activity.
    // For iOS, this returns Zero.
    private static IntPtr GetPlayerActivity() {
#if UNITY_ANDROID
      UnityEngine.AndroidJavaClass jc = new UnityEngine.AndroidJavaClass(
        "com.unity3d.player.UnityPlayer");
      return jc.GetStatic<UnityEngine.AndroidJavaObject>("currentActivity")
               .GetRawObject();
#else
      return IntPtr.Zero;
#endif
    }
  }
}
