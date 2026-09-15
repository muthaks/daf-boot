const { withNativeFederation, shareAll, share } = require('@angular-architects/native-federation/config');

// Framework packages must keep ALL secondary entry points in the shared graph:
// unused-dependency pruning otherwise silently re-bundles pieces (e.g.
// @angular/common's PlatformLocation) privately into remote chunks, splitting
// the singleton and breaking DI when this app loads inside the platform shell.
const frameworkShare = {
  singleton: true,
  strictVersion: false,
  requiredVersion: 'auto',
  includeSecondaries: { keepAll: true },
  build: 'package',
};

module.exports = withNativeFederation({
  name: 'daf',

  exposes: {
    // Platform remote contract: the shell mounts these (design-system/docs/platform-architecture.md).
    './RemoteRoutes': './src/app/remote/remote.routes.ts',
    './SettingsRoutes': './src/app/remote/settings.routes.ts',
  },

  sharedMappings: [],

  shared: {
    // strictVersion false everywhere (remote contract): shell and remotes
    // build independently and drift by patch version; strict matching would
    // split singletons and break cross-remote DI on any mismatch.
    ...shareAll({ singleton: true, strictVersion: false, requiredVersion: 'auto' }),
    ...share({
      '@angular/core': frameworkShare,
      '@angular/common': frameworkShare,
      '@angular/platform-browser': frameworkShare,
      '@angular/router': frameworkShare,
      '@angular/forms': frameworkShare,
    }),
    'co-ds': { singleton: true, strictVersion: false, requiredVersion: '>=1.0.0' },
  },

  skip: [
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket',
    // Theme presets use wildcard subpath exports the import map can't
    // enumerate — bundle them into whatever imports them (co-ds) instead.
    /^@primeuix\/themes/,
    // CJS packages lose named exports when shared via the import map.
    /^@xterm\//,
    // Client-side exports of the agenda meeting page (CommonJS builds): bundle them privately.
    'jspdf',
    'jspdf-autotable',
    'xlsx',
  ],

  features: {
    ignoreUnusedDeps: true,
  },
});
