// <copyright file="BaseObject.cs" company="Google Inc.">
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
  using System.Runtime.InteropServices;
  using System.Text;
  using UnityEngine;

  /// <summary>
  /// Base object manages the pointer to a native object which provides the
  /// implementation of a C# object.
  /// </summary>
  internal abstract class BaseObject : IDisposable {
    // handle to native object.
    private HandleRef selfHandleRef;
    private static HandleRef nullSelf = new HandleRef();

    public BaseObject(IntPtr intPtr) {
      selfHandleRef = new HandleRef(this, intPtr);
    }

    protected HandleRef SelfPtr() {
      if (selfHandleRef.Equals(nullSelf)) {
        throw new InvalidOperationException(
          "Attempted to use object after it was cleaned up");
      }
      return selfHandleRef;
    }

    public virtual void Dispose() {
      selfHandleRef = nullSelf;
    }

    internal delegate UIntPtr OutStringMethod([In, Out] byte[] out_bytes,
        UIntPtr out_size);

    internal static String OutParamsToString(OutStringMethod outStringMethod) {
      UIntPtr requiredSize = outStringMethod(null, UIntPtr.Zero);
      if (requiredSize.Equals(UIntPtr.Zero)) {
        return null;
      }

      string str = null;
      try {
        byte[] array = new byte[requiredSize.ToUInt32()];
        outStringMethod(array, requiredSize);
        str = Encoding.UTF8.GetString(array, 0,
                (int)requiredSize.ToUInt32() - 1);
      } catch (Exception e) {
        Debug.LogError("Exception creating string from char array: " + e);
        str = string.Empty;
      }
      return str;
    }
  }
}
