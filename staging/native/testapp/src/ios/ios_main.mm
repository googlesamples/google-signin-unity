// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#import <UIKit/UIKit.h>

#include <stdarg.h>
#include <string>

#include "main.h"

extern "C" int common_main(int argc, const char* argv[]);

@interface AppDelegate : UIResponder<UIApplicationDelegate>

@property(nonatomic, strong) UIWindow *window;

@end

@interface FTAViewController : UIViewController

@property(atomic, strong) NSString *textEntryResult;

@end

static int g_exit_status = 0;
static bool g_shutdown = false;
static NSCondition *g_shutdown_complete;
static NSCondition *g_shutdown_signal;
static UITextView *g_text_view;
static UIView *g_parent_view;
static FTAViewController *g_view_controller;

@implementation FTAViewController

- (void)viewDidLoad {
  [super viewDidLoad];
  g_parent_view = self.view;
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    const char *argv[] = {"FIREBASE_TESTAPP_NAME"};
    [g_shutdown_signal lock];
    g_exit_status = common_main(1, argv);
    [g_shutdown_complete signal];
  });
}

@end

bool ProcessEvents(int msec) {
  [g_shutdown_signal
      waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:static_cast<float>(msec) / 1000.0f]];
  return g_shutdown;
}

WindowContext GetWindowContext() {
  return g_parent_view;
}

// Log a message that can be viewed in the console.
void LogMessage(const char* format, ...) {
  va_list args;
  NSString *formatString = @(format);

  va_start(args, format);
  NSString *message = [[NSString alloc] initWithFormat:formatString arguments:args];
  va_end(args);

  NSLog(@"%@", message);
  message = [message stringByAppendingString:@"\n"];

  dispatch_async(dispatch_get_main_queue(), ^{
    g_text_view.text = [g_text_view.text stringByAppendingString:message];
  });
}

int main(int argc, char* argv[]) {
  @autoreleasepool {
    UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));
  }
  return g_exit_status;
}

// Create an alert dialog via UIAlertController, and prompt the user to enter a line of text.
// This function spins until the text has been entered (or the alert dialog was canceled).
// If the user cancels, returns an empty string.
std::string ReadTextInput(const char *title, const char *message, const char *placeholder) {
  assert(g_view_controller);
  // This should only be called from a background thread, as it blocks, which will mess up the main
  // thread.
  assert(![NSThread isMainThread]);

  g_view_controller.textEntryResult = nil;

  dispatch_async(dispatch_get_main_queue(), ^{
    UIAlertController *alertController =
        [UIAlertController alertControllerWithTitle:@(title)
                                            message:@(message)
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alertController addTextFieldWithConfigurationHandler:^(UITextField *_Nonnull textField) {
      textField.placeholder = @(placeholder);
    }];
    UIAlertAction *confirmAction = [UIAlertAction
        actionWithTitle:@"OK"
                  style:UIAlertActionStyleDefault
                handler:^(UIAlertAction *_Nonnull action) {
                  g_view_controller.textEntryResult = alertController.textFields.firstObject.text;
                }];
    [alertController addAction:confirmAction];
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"Cancel"
                                                           style:UIAlertActionStyleCancel
                                                         handler:^(UIAlertAction *_Nonnull action) {
                                                           g_view_controller.textEntryResult = @"";
                                                         }];
    [alertController addAction:cancelAction];
    [g_view_controller presentViewController:alertController animated:YES completion:nil];
  });

  while (true) {
    // Pause a second, waiting for the user to enter text.
    if (ProcessEvents(1000)) {
      // If this returns true, an exit was requested.
      return "";
    }
    if (g_view_controller.textEntryResult != nil) {
      // textEntryResult will be set to non-nil when a dialog button is clicked.
      std::string result = g_view_controller.textEntryResult.UTF8String;
      g_view_controller.textEntryResult = nil;  // Consume the result.
      return result;
    }
  }
}

@implementation AppDelegate

- (BOOL)application:(UIApplication*)application
    didFinishLaunchingWithOptions:(NSDictionary*)launchOptions {
  g_shutdown_complete = [[NSCondition alloc] init];
  g_shutdown_signal = [[NSCondition alloc] init];
  [g_shutdown_complete lock];

  self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
  g_view_controller = [[FTAViewController alloc] init];
  self.window.rootViewController = g_view_controller;
  [self.window makeKeyAndVisible];

  g_text_view = [[UITextView alloc] initWithFrame:g_view_controller.view.bounds];

  g_text_view.editable = NO;
  g_text_view.scrollEnabled = YES;
  g_text_view.userInteractionEnabled = YES;

  [g_view_controller.view addSubview:g_text_view];

  return YES;
}

- (void)applicationWillTerminate:(UIApplication *)application {
  g_view_controller = nil;
  g_shutdown = true;
  [g_shutdown_signal signal];
  [g_shutdown_complete wait];
}

@end
