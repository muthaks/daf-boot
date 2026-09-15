import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";
import { TokenStorageService } from "../services/token-storage.service";

/** Standalone only: a 401 from DAF's agenda API means the DAF token is gone or expired. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const tokens = inject(TokenStorageService);
  return next(req).pipe(
    catchError((error) => {
      if (error?.status === 401 && /\/approvalForum\//.test(req.url)) {
        tokens.clear();
        void router.navigate(["/login"], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => error);
    }),
  );
};
