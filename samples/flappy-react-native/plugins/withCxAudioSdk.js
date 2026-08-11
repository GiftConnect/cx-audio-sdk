const fs = require('fs');
const path = require('path');
const { withDangerousMod } = require('@expo/config-plugins');

const POD_LINE = "  pod 'CxAudioSDK', :path => '../node_modules/cx-audio-sdk'";

module.exports = function withCxAudioSdk(config) {
  return withDangerousMod(config, [
    'ios',
    async (modConfig) => {
      const podfilePath = path.join(modConfig.modRequest.platformProjectRoot, 'Podfile');
      let podfile = fs.readFileSync(podfilePath, 'utf8');
      if (!podfile.includes("pod 'CxAudioSDK'")) {
        const targetPattern = /(target\s+['\"][^'\"]+['\"]\s+do\s*\n)/;
        if (!targetPattern.test(podfile)) {
          throw new Error('Unable to find the application target in the generated Podfile');
        }
        podfile = podfile.replace(targetPattern, `$1${POD_LINE}\n`);
        fs.writeFileSync(podfilePath, podfile);
      }
      return modConfig;
    },
  ]);
};
