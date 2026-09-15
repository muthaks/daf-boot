import { inject } from "@angular/core";
import { CanActivateChildFn, Router } from "@angular/router";
import { TokenStorageService } from "../services/token-storage.service";

/** Standalone chrome: no DAF session → the app's own login, remembering where the user was going. */
export const authGuard: CanActivateChildFn = (_route, state) => {
  const tokens = inject(TokenStorageService);
  if (tokens.isLoggedIn()) return true;
  return inject(Router).createUrlTree(["/login"], {
    queryParams: { returnUrl: state.url },
  });
};
