import { inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { defer, switchMap } from "rxjs";
import { loadDafConfig } from "./daf-config.service";
import { TokenStorageService } from "./token-storage.service";

/**
 * Addresses DAF's API through the gateway prefix from assets/config.json and attaches the DAF
 * role token as bearer. DAF is not an audience of the auth service, so the shell cannot exchange a
 * token for it: standalone or hosted, the token this app obtained (SSO → DAF /auth/ssoToken) is
 * the credential, and this client is the one place it is sent. Requests are addressed as
 * /auth/... or /approvalForum/... ; the sign-in endpoints themselves carry no token.
 */
@Injectable({ providedIn: "root" })
export class DafHttpClient extends HttpClient {
  constructor() {
    const inner = inject(HttpClient);
    const tokens = inject(TokenStorageService);
    super({
      handle: (request) =>
        defer(loadDafConfig).pipe(
          switchMap((config) => {
            if (!/^\/(auth|approvalForum)(?:\/|\?|$)/.test(request.url)) {
              throw new Error("Committee Agendas requests must use the /auth or /approvalForum path.");
            }
            const token = request.url.startsWith("/auth/") ? null : tokens.getToken();
            return inner.request(
              request.clone({
                url: config.apiUrl + request.url,
                withCredentials: false,
                ...(token ? { setHeaders: { Authorization: `Bearer ${token}` } } : {}),
              }),
            );
          }),
        ),
    });
  }
}
