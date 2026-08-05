Pod::Spec.new do |spec|
  spec.name = "CxAudioSDK"
  spec.module_name = "CxAudioSDK"
  spec.version = "0.1.0"
  spec.summary = "Native iOS SDK for CX audio ad placements."
  spec.description = <<-DESC
    Native iOS implementation of the CX Audio SDK, including background audio,
    sticky player, and fullscreen interstitial placements.
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
  spec.source_files = "ios/Sources/CxAudioSDK/**/*.{swift}"
  spec.frameworks = "AVFoundation", "UIKit"
end
