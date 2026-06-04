// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "CxAudioSDK",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "CxAudioSDK",
            targets: ["CxAudioSDK"]
        )
    ],
    targets: [
        .target(
            name: "CxAudioSDK",
            path: "ios/Sources/CxAudioSDK"
        ),
        .testTarget(
            name: "CxAudioSDKTests",
            dependencies: ["CxAudioSDK"],
            path: "ios/Tests/CxAudioSDKTests"
        )
    ]
)
