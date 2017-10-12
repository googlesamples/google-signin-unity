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

#ifndef GOOGLESIGNIN_FUTURE_H  // NOLINT
#define GOOGLESIGNIN_FUTURE_H

namespace googlesignin {

// Provides a future promise for an asynchronous result of type <T>
template <class T>
class Future {
 public:
  virtual ~Future() {}
  // Returns the Status of the operation.  This is set once Pending is false.
  virtual int Status() const = 0;

  // Returns the Result of the operation if successful.  If it is not
  // successful, the error code should be retrieved using Status().  The result
  // is available once Pending() is false.
  virtual T* Result() const = 0;

  // Returns true while the promise has not been fulfilled by the operation.
  // Once it is false, the Status and Result fields are populated.
  virtual bool Pending() const = 0;
};

}  // namespace googlesignin

#endif  // GOOGLESIGNIN_FUTURE_H  NOLINT
