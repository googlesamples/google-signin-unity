// <copyright file="GoogleSignInDependencies.cs" company="Google Inc.">
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

namespace GoogleSignIn.Editor {
  using System;
  using System.Collections.Generic;
  using UnityEditor;
  using UnityEngine;

  /// <summary>
  /// Google sign in dependencies.  This class registers the dependendies
  /// on Google Play Services with the JarResolver plugin.
  /// </summary>
  [InitializeOnLoad]
  public class GoogleSignInDependencies : AssetPostprocessor {

    static GoogleSignInDependencies() {
      AddDependencies();
    }

#if UNITY_ANDROID
    public static object svcSupport;

    static void AddDependencies() {
      // Setup the resolver using reflection as the module may not be
      // available at compile time.
      Type playServicesSupport = Google.VersionHandler.FindClass(
        "Google.JarResolver", "Google.JarResolver.PlayServicesSupport");
      if (playServicesSupport == null) {
        return;
      }
      svcSupport = svcSupport ?? Google.VersionHandler.InvokeStaticMethod(
        playServicesSupport, "CreateInstance",
        new object[] {"GoogleSignIn",
                    EditorPrefs.GetString("AndroidSdkRoot"),
                    "ProjectSettings"}
      );
      if (svcSupport != null) {
        // The only direct dependency is on auth, min version 10.2.0.
        Google.VersionHandler.InvokeInstanceMethod(svcSupport,
                "DependOn", new object[] {
                            "com.google.android.gms",
                            "play-services-auth",
                            "10.2+" },
                    namedArgs: new Dictionary<string, object>() {
                                {"packageIds", new string[] {
                                    "extra-google-m2repository"
                                    }
                                }
                    });
      } else {
        Debug.LogError("Jar resolver service not available.  " +
          "Please add the Jar resolver to your project or ignore this error " +
          " and manage the Google Play Services dependencies manually");
      }
    }
#elif UNITY_IOS
    static void AddDependencies() {
            Type iosResolver = Google.VersionHandler.FindClass(
      "Google.IOSResolver", "Google.IOSResolver");
            if (iosResolver == null) {
                return;
            }

            Google.VersionHandler.InvokeStaticMethod(
          iosResolver, "AddPod",
          new object[] { "GoogleSignIn" },
          namedArgs: new Dictionary<string, object>() {
          { "version", ">=  4.0.2" },
          { "bitcodeEnabled", false },
          });
    }
#else
    static void AddDependencies() { }
#endif  // UNITY_ANDROID / iOS
  }
}
