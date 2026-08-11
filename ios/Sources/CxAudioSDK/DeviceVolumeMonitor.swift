import AVFoundation
import Foundation

internal enum DeviceVolumeMonitor {
    static func isDeviceVolumeAllowed(minDeviceVolume: Float) -> Bool {
        guard minDeviceVolume > 0 else {
            return true
        }

        #if os(iOS) || os(tvOS) || os(watchOS)
        return AVAudioSession.sharedInstance().outputVolume >= minDeviceVolume
        #else
        return true
        #endif
    }
}
