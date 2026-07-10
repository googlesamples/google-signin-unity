#if UNITY_EDITOR || UNITY_ANDROID || UNITY_IOS
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
	using System.Text;
    using System.Linq;
	using System.Collections.Generic;
	using System.Runtime.InteropServices;

	using UnityEngine;
#if UNITY_2022_2_OR_NEWER
#else
	using System.Reflection;
#endif

    internal class GoogleSignInImpl : BaseObject, ISignInImpl {

	internal GoogleSignInImpl(GoogleSignInConfiguration configuration) : base(GoogleSignIn_Create(GetPlayerActivity())) {

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

#if UNITY_ANDROID
	static AndroidJavaClass GoogleSignInHelper = new AndroidJavaClass("com.google.googlesignin.GoogleSignInHelper");

	static IntPtr GoogleSignIn_Create(IntPtr activity)
	{
		return new AndroidJavaObject("com.google.googlesignin.GoogleSignInHelper").GetRawObject();
	}

	static bool GoogleSignIn_Configure(HandleRef googleSignInHelper,
		bool useGameSignIn, string webClientId,
		bool requestAuthCode, bool forceTokenRefresh, bool requestEmail,
		bool requestIdToken, bool hidePopups, string[] additionalScopes,
		int scopeCount, string accountName)
	{
		GoogleSignInHelper.CallStatic("configure",
			useGameSignIn,
			webClientId,
			requestAuthCode,
			forceTokenRefresh,
			requestEmail,
			requestIdToken,
			hidePopups,
			accountName,
			additionalScopes,
			new SignInListener());

		return !useGameSignIn;
	}

	static AndroidJavaObject googleIdTokenCredential;
	static AndroidJavaObject authorizationResult;

	public class SignInListener : AndroidJavaProxy
	{
		public SignInListener() : base("com.google.googlesignin.IListener") { }

	    public void onAuthenticated(AndroidJavaObject _googleIdTokenCredential)
		{
			googleIdTokenCredential?.Dispose();
			googleIdTokenCredential = _googleIdTokenCredential;
		}

    	public void onAuthorized(AndroidJavaObject _authorizationResult)
		{
			authorizationResult?.Dispose();
			authorizationResult = _authorizationResult;
		}

		public void onFailure(AndroidJavaObject exception)
		{
			Debug.LogErrorFormat("onFailure {0} : {1}",exception?.Call<AndroidJavaObject>("getClass").Call<string>("toString"),exception?.Call<string>("getMessage"));
			exception.Dispose();
		}

		public void onCanceled() {
			googleIdTokenCredential?.Dispose();
			authorizationResult?.Dispose();
		}
	}

	static void GoogleSignIn_EnableDebugLogging(HandleRef self, bool flag) => GoogleSignInHelper.CallStatic("enableDebugLogging",flag);

	static IntPtr GoogleSignIn_SignIn(HandleRef self)
	{
		return GoogleSignInHelper.CallStatic<AndroidJavaObject>("signIn").GetRawObject();
	}

	static IntPtr GoogleSignIn_SignInSilently(HandleRef self)
	{
		return GoogleSignInHelper.CallStatic<AndroidJavaObject>("signInSilently").GetRawObject();
	}

	static void GoogleSignIn_Signout(HandleRef self)
	{
		googleIdTokenCredential?.Dispose();
		googleIdTokenCredential = null;

		authorizationResult?.Dispose();
		authorizationResult = null;

		GoogleSignInHelper.CallStatic("signOut");
	}

	static void GoogleSignIn_Disconnect(HandleRef self) => throw new NotImplementedException();

	internal static void GoogleSignIn_DisposeFuture(HandleRef self) => GoogleSignInHelper.CallStatic("cancel");

	internal static bool GoogleSignIn_Pending(HandleRef self) => GoogleSignInHelper.CallStatic<bool>("isPending");

	internal static IntPtr GoogleSignIn_Result(HandleRef self) => googleIdTokenCredential.GetRawObject();

	internal static int GoogleSignIn_Status(HandleRef self) => GoogleSignInHelper.CallStatic<int>("getStatus");

	internal static string GoogleSignIn_GetServerAuthCode(HandleRef self) => authorizationResult?.Call<string>("getServerAuthCode");

	internal static string GoogleSignIn_GetUserId(HandleRef self)
	{
		try
		{
			string idTokenFull = googleIdTokenCredential?.Call<string>("getIdToken");
			string idTokenPart = idTokenFull?.Split('.')?.ElementAtOrDefault(1);
			if(!(idTokenPart?.Length > 1))
				return null;

			// Replace URL-safe characters and fix padding
			idTokenPart = idTokenPart.Replace('-', '+').Replace('_', '/');
			int mod = idTokenPart.Length % 4;
			if(mod > 0)
				idTokenPart += new string('=',4 - mod);
			var idTokenFromBase64 = Convert.FromBase64String(idTokenPart);
			var idToken = Encoding.UTF8.GetString(idTokenFromBase64);
			var jobj = Newtonsoft.Json.Linq.JObject.Parse(idToken);
			return jobj?["sub"]?.ToString();
		}
		catch(Exception e)
		{
			Debug.LogException(e);
			return null;
		}
	}

	internal static string GoogleSignIn_GetEmail(HandleRef self) => googleIdTokenCredential?.Call<string>("getId");

	internal static string GoogleSignIn_GetDisplayName(HandleRef self) => googleIdTokenCredential?.Call<string>("getDisplayName");

	internal static string GoogleSignIn_GetFamilyName(HandleRef self) => googleIdTokenCredential?.Call<string>("getFamilyName");

	internal static string GoogleSignIn_GetGivenName(HandleRef self) => googleIdTokenCredential?.Call<string>("getGivenName");

	internal static string GoogleSignIn_GetIdToken(HandleRef self) => googleIdTokenCredential?.Call<string>("getIdToken");

	internal static string GoogleSignIn_GetImageUrl(HandleRef self) => googleIdTokenCredential?.Call<AndroidJavaObject>("getProfilePictureUri")?.Call<string>("toString");
#else
	private const string DllName = "__Internal";

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
	internal static extern UIntPtr GoogleSignIn_GetDisplayName(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetEmail(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetFamilyName(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetGivenName(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetIdToken(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetImageUrl(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	[DllImport(DllName)]
	internal static extern UIntPtr GoogleSignIn_GetUserId(HandleRef self, [In, Out] byte[] bytes, UIntPtr len);

	internal static string GoogleSignIn_GetServerAuthCode(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetServerAuthCode(self, out_string, out_size));

	internal static string GoogleSignIn_GetDisplayName(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetDisplayName(self, out_string, out_size));

	internal static string GoogleSignIn_GetEmail(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetEmail(self, out_string, out_size));

	internal static string GoogleSignIn_GetFamilyName(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetFamilyName(self, out_string, out_size));

	internal static string GoogleSignIn_GetGivenName(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetGivenName(self, out_string, out_size));

	internal static string GoogleSignIn_GetIdToken(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetIdToken(self, out_string, out_size));

	internal static string GoogleSignIn_GetImageUrl(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetImageUrl(self, out_string, out_size));

	internal static string GoogleSignIn_GetUserId(HandleRef self) =>
		OutParamsToString((out_string, out_size) => GoogleSignIn_GetUserId(self, out_string, out_size));
#endif

		// Gets the Unity player activity.
		// For iOS, this returns Zero.
		private static IntPtr GetPlayerActivity() {
#if UNITY_ANDROID
			var jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
			return jc.GetStatic<AndroidJavaObject>("currentActivity").GetRawObject();
#else
			return IntPtr.Zero;
#endif
		}
	}

	public static class Ext
	{
#if UNITY_2022_2_OR_NEWER
#else
		static ConstructorInfo constructorInfo;
#endif

		public static AndroidJavaObject ToAndroidJavaObject(in this HandleRef self) => self.Handle.ToAndroidJavaObject();
		public static AndroidJavaObject ToAndroidJavaObject(in this IntPtr intPtr)
		{
			if (intPtr == IntPtr.Zero)
				return null;

			try {
#if UNITY_2022_2_OR_NEWER
				return new AndroidJavaObject(intPtr);
#else
				if(constructorInfo == null)
					constructorInfo = typeof(AndroidJavaObject).GetConstructor(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic,null,new[] { typeof(IntPtr) },null);

				Debug.LogFormat("constructorInfo : {0}",constructorInfo);
				return constructorInfo.Invoke(new object[] { intPtr }) as AndroidJavaObject;
#endif
			} catch (Exception e) {
				Debug.LogError("Exception creating AndroidJavaObject: " + e);
				return null;
			}
		}
	}
}
#endif
