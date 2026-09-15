import { computed, inject, Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Router } from "@angular/router";
import { catchError, forkJoin, map, Observable, of, switchMap, tap, throwError } from "rxjs";
import { environment } from "../../../environments/environment";
import { getRemoteHost } from "../../remote/remote-host";
import {
  DafTenantResponse,
  DafTokenResponse,
  DafUser,
  SsoCredentialResponse,
  WindowsAuthDetail,
} from "../models/auth.models";
import { DafHttpClient } from "./daf-http-client";
import { TokenStorageService, tokenIdentity } from "./token-storage.service";

const SSO_FAILED_KEY = "daf.sso_failed";

/**
 * Sign-in to DAF, the way DAF's own UI does it (docs/AUTHENTICATION.md):
 *
 * 1. `GET /auth/ssoServers` on DAF lists the Windows SSO servers.
 * 2. `GET <sso server>userdetail` (with the Windows session cookie) describes the user, or
 *    `POST <sso server>loginpost` checks typed credentials and returns the same description.
 * 3. `POST /auth/tenant` and `POST /auth/ssoToken` on DAF turn that description into DAF's role
 *    token, which the agenda API accepts as bearer.
 *
 * Hosted in the Credit shell the user is already signed in to the shell; step 2 runs silently,
 * so the user signs in once. DAF is not an auth-service audience, which is why the shell's
 * token exchange is not used here.
 */
@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly raw = inject(HttpClient);
  private readonly daf = inject(DafHttpClient);
  private readonly tokens = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly currentUser = signal<DafUser | null>(
    this.tokens.isLoggedIn() ? this.tokens.getUser() : null,
  );
  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  /** The identity to show: the shell's user when hosted, otherwise the DAF session. */
  readonly displayName = computed(
    () => getRemoteHost()?.user()?.displayName ?? this.currentUser()?.displayName ?? "",
  );

  /** Configured SSO server URLs from DAF (empty = SSO not configured). */
  loadSsoServers(): Observable<string[]> {
    return this.daf.get<string[]>("/auth/ssoServers").pipe(
      map((servers) => (Array.isArray(servers) ? servers.filter(Boolean) : [])),
      catchError(() => of([] as string[])),
    );
  }

  /** Tries each SSO server in order for the Windows user's detail; null when none answers. */
  fetchUserDetail(servers: string[]): Observable<WindowsAuthDetail | null> {
    const tryServer = (index: number): Observable<WindowsAuthDetail | null> => {
      if (index >= servers.length) return of(null);
      return this.raw
        .get<WindowsAuthDetail>(endpoint(servers[index], "userdetail"), { withCredentials: true })
        .pipe(
          map((detail) => (detail?.userPrincipalName ? detail : null)),
          catchError(() => tryServer(index + 1)),
        );
    };
    return servers.length ? tryServer(0) : of(null);
  }

  /** Silent Windows SSO end to end: discover → describe → DAF token. Null when unavailable. */
  attemptSso(): Observable<DafUser | null> {
    if (this.ssoFailed()) return of(null);
    return this.loadSsoServers().pipe(
      switchMap((servers) => (servers.length ? this.fetchUserDetail(servers) : of(null))),
      switchMap((detail) => (detail ? this.establishSession(detail) : of(null))),
      tap((user) => this.markSsoFailed(!user)),
      catchError(() => {
        this.markSsoFailed(true);
        return of(null);
      }),
    );
  }

  /** Credentials checked by the SSO server, then the same DAF token exchange. */
  login(username: string, password: string): Observable<DafUser> {
    return this.loadSsoServers().pipe(
      switchMap((servers) => {
        if (!servers.length)
          return throwError(() => new Error("No sign-in server is configured for DAF."));
        return this.raw.post<SsoCredentialResponse>(endpoint(servers[0], "loginpost"), {
          domain: environment.domain,
          username,
          password,
        });
      }),
      switchMap((response) =>
        response?.isValid && response.userDetail
          ? this.establishSession(response.userDetail)
          : throwError(
              () =>
                new Error(
                  response?.errorMessage || "Sign-in failed. Check your username and password.",
                ),
            ),
      ),
    );
  }

  /** `POST /auth/tenant` + `POST /auth/ssoToken` on DAF; stores the token and the user it names. */
  establishSession(detail: WindowsAuthDetail): Observable<DafUser> {
    return forkJoin({
      tenant: this.daf
        .post<DafTenantResponse>("/auth/tenant", detail)
        .pipe(catchError(() => of({ tenantId: null } as unknown as DafTenantResponse))),
      token: this.daf.post<DafTokenResponse>("/auth/ssoToken", detail),
    }).pipe(
      map(({ tenant, token }) => {
        const identity = tokenIdentity(token?.Token ?? "");
        if (!identity) throw new Error("DAF did not issue a session token.");
        const user: DafUser = {
          username: identity.username,
          displayName: detail.displayName || identity.username,
          domain: detail.domain || environment.domain,
          roles: identity.roles,
          tenantId: typeof tenant?.tenantId === "number" ? tenant.tenantId : null,
        };
        this.tokens.saveSession(token.Token, user);
        this.currentUser.set(user);
        this.markSsoFailed(false);
        return user;
      }),
    );
  }

  ssoFailed(): boolean {
    try {
      return sessionStorage.getItem(SSO_FAILED_KEY) === "1";
    } catch {
      return false;
    }
  }

  markSsoFailed(failed: boolean): void {
    try {
      if (failed) sessionStorage.setItem(SSO_FAILED_KEY, "1");
      else sessionStorage.removeItem(SSO_FAILED_KEY);
    } catch {
      /* ignore */
    }
  }

  logout(): void {
    this.tokens.clear();
    this.currentUser.set(null);
    // Stop silent SSO from signing straight back in on this tab.
    this.markSsoFailed(true);
    void this.router.navigate(["/login"]);
  }
}

function endpoint(server: string, path: string): string {
  const base = server.trim();
  return base.endsWith("/") ? `${base}${path}` : `${base}/${path}`;
}
