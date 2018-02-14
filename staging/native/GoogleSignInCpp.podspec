#
# Be sure to run `pod lib lint GoogleSignInCpp.podspec' to ensure this is a
# valid spec before submitting.
#
# Any lines starting with a # are optional, but their use is encouraged
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
  s.name             = 'GoogleSignInCpp'
  s.version          = '0.1.0'
  s.summary          = 'Google Sign In C++ SDK for iOS'

  s.description      = <<-DESC
Google Sign-In is a secure authentication system that reduces the burden of login for your users, by enabling them to sign in with their Google account—the same account they already use with Gmail, Play, Google+, and other Google services.
This C++ SDK wraps the iOS SDK, as well as the android SDK, providing a platform agnostic API.
                       DESC

  s.homepage         = 'https://developers.google.com/identity/'
  s.license          = { :type => 'Apache', :file => 'LICENSE' }
  s.authors          = 'Google, Inc.'

  s.source           = { :git => 'https://github.com/TBD/GoogleSignInCpp.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'

  s.ios.deployment_target = '7.0'

  s.cocoapods_version = '>= 1.4.0.beta.2'
  s.static_framework = true
  s.prefix_header_file = false

  s.source_files = 'src/ios/*'
  s.source_files = 'src/include/*'
  s.requires_arc = 'src/ios/*'
  s.public_header_files = 'src/include/*.h'

  s.ios.dependency 'GoogleSignIn', '~> 4.1'

  s.library = 'c++'
end