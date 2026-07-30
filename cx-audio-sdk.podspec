Pod::Spec.new do |spec|
  spec.name = "cx-audio-sdk"
  spec.module_name = "CxAudioSDK"
  spec.version = "0.1.0"
  spec.summary = "CX Audio SDK React Native bindings and native iOS implementation."
  spec.homepage = "https://github.com/GiftConnect/cx-audio-sdk"
  spec.license = { :type => "Apache-2.0", :file => "LICENSE" }
  spec.author = "Consumable"
  spec.source = {
    :git => "https://github.com/GiftConnect/cx-audio-sdk.git",
    :tag => spec.version.to_s
  }

  spec.platform = :ios, "15.0"
  spec.swift_version = "5.9"
  spec.source_files = [
    "ios/Sources/CxAudioSDK/**/*.{swift}",
    "react-native/ios/**/*.{h,m,mm,swift}"
  ]
  spec.frameworks = "AVFoundation", "UIKit"
  spec.dependency "React-Core"
end
