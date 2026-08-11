const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const sdkRoot = path.resolve(projectRoot, '../..');
const config = getDefaultConfig(projectRoot);

// The SDK is installed from the repository root while developing this sample.
// Metro must watch that directory and resolve all peer dependencies from the
// sample so React and React Native are not loaded more than once.
config.watchFolders = [sdkRoot];
config.resolver.nodeModulesPaths = [path.resolve(projectRoot, 'node_modules')];

module.exports = config;
