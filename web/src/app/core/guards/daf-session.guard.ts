import { inject } from "@angular/core";
import { CanActivateChildFn, Router } from "@angular/router";
import { getRemoteHost } from "../../remote/remote-host";
import { PermissionsService } from "../services/permissions.service";
import { SessionService } from "../services/session.service";
import { TokenStorageService } from "../services/token-storage.service";

/**
 * Session guard for the routes the Credit shell mounts. MUST fail soft: hosted, the DAF session
 * is established silently through Windows SSO (`SessionService.ensure`) and every page renders
 * its own state while that happens or if it fails. Standalone deep links without a session
 * bounce to the app's login.
 */
export const dafSessionGuard: CanActivateChildFn = (_route, state) => {
  const session = inject(SessionService);
  inject(PermissionsService); // publishes this identity's permissions to the shell menu once known
  if (getRemoteHost()) {
    session.ensure();
    return true;
  }
  if (!inject(TokenStorageService).isLoggedIn()) {
    return inject(Router).createUrlTree(["/login"], {
      queryParams: { returnUrl: state.url },
    });
  }
  session.ensure();
  return true;
};
