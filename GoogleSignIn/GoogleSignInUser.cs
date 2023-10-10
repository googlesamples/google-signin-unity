// <copyright file="GoogleSignInUser.cs" company="Google Inc.">
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
  using System;

    /// <summary> Information for the authenticated user.</summary>
    public class GoogleSignInUser {

    /// <summary> Server AuthCode to be exchanged for an auth token.</summary>
    ///<remarks> null if not requested, or if there was an error.</remarks>
    public string AuthCode {
      get;
      internal set;
    }

    /// <summary> Email address.</summary>
    ///<remarks> null if not requested, or if there was an error.</remarks>
    public string Email {
      get;
      internal set;
    }

    /// <summary> Id token.</summary>
    ///<remarks> null if not requested, or if there was an error.</remarks>
    public string IdToken {
      get;
      internal set;
    }

    /// <summary> Display Name.</summary>
    public string DisplayName {
      get;
      internal set;
    }

    /// <summary> Given Name.</summary>
    public string GivenName {
      get;
      internal set;
    }

    /// <summary> Family Name.</summary>
    public string FamilyName {
      get;
      internal set;
    }

    /// <summary> Profile photo</summary>
    /// <remarks> Can be null if the profile is not requested,
    /// or none set.</remarks>
    public Uri ImageUrl {
      get;
      internal set;
    }

    /// <summary> User ID</summary>
    public string UserId {
      get;
      internal set;
    }
  }
}
