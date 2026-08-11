Pod::Spec.new do |spec|
  spec.name = "CxAudioSDK"
  spec.module_name = "CxAudioSDK"
  spec.version = "0.1.1"
  spec.summary = "Native iOS SDK for CX audio ad placements."
  spec.description = <<-DESC
    Native iOS implementation of the CX Audio SDK, including background audio,
    sticky player, and fullscreen interstitial placements.
  DESC
  spec.homepage = "https://consumable.com/"
  spec.readme = "https://raw.githubusercontent.com/GiftConnect/cx-audio-sdk/#{spec.version}/ios/README.md"
  spec.documentation_url = "https://github.com/GiftConnect/cx-audio-sdk/blob/#{spec.version}/ios/README.md"
  spec.license = { :type => "Apache-2.0", :file => "LICENSE" }
  spec.author = "Consumable"
  spec.source = {
    :git => "https://github.com/GiftConnect/cx-audio-sdk.git",
    :tag => spec.version.to_s
  }

  spec.platform = :ios, "15.0"
  spec.swift_version = "5.9"
  spec.source_files = "ios/Sources/CxAudioSDK/**/*.{swift}"
  spec.resource_bundles = {
    "CxAudioSDK_Privacy" => ["ios/Sources/CxAudioSDK/PrivacyInfo.xcprivacy"]
  }
  spec.frameworks = "AVFoundation", "UIKit"
end
