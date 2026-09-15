import { initFederation } from "@angular-architects/native-federation";

// Standalone development needs the same shared-package import map as the host.
// The shell loads RemoteRoutes directly and does not execute this entrypoint.
initFederation()
  .then(() => import("./bootstrap"))
  .catch((err) => console.error(err));
