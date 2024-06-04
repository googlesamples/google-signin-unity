/**
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#import <GoogleSignIn/GIDSignIn.h>
#import <GoogleSignIn/GIDConfiguration.h>

@interface GoogleSignInHandler : NSObject
{
    @public
    GIDConfiguration* signInConfiguration;

    @public
    NSString* loginHint;
    
    @public
    NSMutableArray* additionalScopes;
}

@property(class, nonatomic, readonly) GoogleSignInHandler *sharedInstance;

- (void)signIn:(GIDSignIn *)signIn dismissViewController:(UIViewController *)viewController;

- (void)signIn:(GIDSignIn *)signIn didSignInForUser:(GIDGoogleUser *)user withError:(NSError *)_error;

- (void)signIn:(GIDSignIn *)signIn didDisconnectWithUser:(GIDGoogleUser *)user withError:(NSError *)_error;

@end
