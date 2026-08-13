Pod::Spec.new do |spec|
  spec.name = "cx-audio-sdk"
  spec.module_name = "CxAudioReactNative"
  spec.version = "0.1.2"
  spec.summary = "React Native bindings for the CX Audio SDK."
  spec.description = <<-DESC
    React Native iOS bindings for the native CxAudioSDK CocoaPod.
  DESC
  spec.homepage = "https://github.com/GiftConnect/cx-audio-sdk"
  spec.license = { :type => "Apache-2.0", :file => "LICENSE" }
  spec.author = "Consumable"
  spec.source = {
    :git => "https://github.com/GiftConnect/cx-audio-sdk.git",
    :tag => spec.version.to_s
  }

  spec.platform = :ios, "15.0"
  spec.swift_version = "5.9"
  spec.source_files = "react-native/ios/**/*.{h,m,mm,swift}"
  spec.frameworks = "UIKit"
  spec.dependency "CxAudioSDK", spec.version.to_s
  spec.dependency "React-Core", ">= 0.71.0"
end
