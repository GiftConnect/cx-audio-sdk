# Publishing the CX Audio SDK

The repository is configured to distribute the native Android SDK through
Maven Central and the native iOS SDK through CocoaPods Trunk. MvnRepository is
an index of Maven repositories, so an artifact published to Maven Central will
be discoverable there after its indexes update.

Release versions are immutable in both registries. Before releasing, update
every repository version together, commit the release, and create a Git tag
whose name exactly matches the version (for example, `0.1.3`). The native
CocoaPod reads its source from that tag.

## Android and Maven Central

The Android artifact coordinates are:

```text
io.github.giftconnect:cx-audio-sdk:0.1.3
```

The `android` module uses the Gradle Maven Publish Plugin to generate the AAR,
POM, source artifact, documentation artifact, and signatures required by
Maven Central.

Before the first release:

1. Create a Central Publisher Portal account.
2. Register and verify the `io.github.giftconnect` namespace.
3. Create a Central Portal user token.
4. Create a GPG signing key and publish its public key.

Keep all credentials outside the repository. For a local release, put them in
the user-level `~/.gradle/gradle.properties` file:

```properties
mavenCentralUsername=TOKEN_USERNAME
mavenCentralPassword=TOKEN_PASSWORD
signing.keyId=KEY_ID
signing.password=KEY_PASSWORD
signing.secretKeyRingFile=/absolute/path/to/secring.gpg
```

CI can instead use the plugin's `ORG_GRADLE_PROJECT_...` environment
variables, including an ASCII-armored in-memory signing key.

Build and inspect the publication locally without uploading or requiring the
release signing key:

```sh
./gradlew :android:assembleRelease \
  :android:generatePomFileForMavenPublication \
  :android:sourceReleaseJar \
  :android:javaDocReleaseJar
```

The generated AAR and supplemental artifacts are under `android/build`, and
the generated POM is under
`android/build/publications/maven/pom-default.xml`. A full
`publishToMavenLocal` invocation also runs signing and therefore requires the
GPG properties listed above.

Upload a signed release to the Central Portal for validation:

```sh
./gradlew :android:publishToMavenCentral
```

After validation, publish it from the Central Portal. To upload and release in
one explicit command instead, use:

```sh
./gradlew :android:publishAndReleaseToMavenCentral
```

## iOS and CocoaPods Trunk

`CxAudioSDK.podspec` publishes the native iOS SDK. The lowercase
`cx-audio-sdk.podspec` is the React Native bridge bundled with the npm package;
do not push it to CocoaPods Trunk. A React Native application's CocoaPods
installation resolves that bridge locally from `node_modules`, where
React Native also supplies its `React-Core` dependency.

Validate the native pod from the repository checkout:

```sh
pod lib lint CxAudioSDK.podspec
```

After the matching release commit and tag have been pushed, validate the
remote source and publish the pod:

```sh
pod spec lint CxAudioSDK.podspec
pod trunk push CxAudioSDK.podspec
```

Publishing requires a registered and verified CocoaPods Trunk session. Check
the current machine with `pod trunk me`; register it with `pod trunk register`
when necessary.

## React Native and npm

The root `package.json` publishes the React Native bindings to npm. Its
included `cx-audio-sdk.podspec` depends on the native `CxAudioSDK` pod at the
same version, so publish the native CocoaPod before publishing the npm package.

Inspect exactly what npm will receive:

```sh
npm run check:react-native
npm pack --dry-run
```

After the matching release commit and tag are public and the native CocoaPod
is available, authenticate with npm and publish:

```sh
npm whoami
npm publish --access public
```
