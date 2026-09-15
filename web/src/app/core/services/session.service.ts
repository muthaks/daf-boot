import { computed, inject, Injectable, signal } from "@angular/core";
import { DafUser } from "../models/auth.models";
import { getRemoteHost } from "../../remote/remote-host";
import { AuthService } from "./auth.service";
import { TokenStorageService } from "./token-storage.service";

/**
 * The DAF session for the current identity. Hosted in the shell it is established silently
 * through Windows SSO the first time a page needs it; a failure is not fatal — pages render and
 * show their own error state (REMOTE-STANDARDS §3, fail soft), with the standalone sign-in as
 * the way out for browsers without Windows SSO.
 */
@Injectable({ providedIn: "root" })
export class SessionService {
  private readonly auth = inject(AuthService);
  private readonly tokens = inject(TokenStorageService);
  private loadedFor: string | null = null;

  readonly session = computed<DafUser | null>(() => this.auth.user());

  /** Human label for the user's DAF standing, for chrome and the home page only (never for gating). */
  readonly roleLabel = computed(() => {
    const roles = (this.auth.user()?.roles ?? []).map((r) => r.toLowerCase());
    const has = (role: string) => roles.some((r) => r.includes(role));
    if (has("chairperson")) return "Chairperson";
    if (has("secretary")) return "Secretary";
    if (has("approver")) return "Approver";
    if (has("reviewanalyst")) return "Review analyst";
    if (has("creditanalyst")) return "Credit analyst";
    return roles.length ? "No agenda role" : "";
  });
  readonly loading = signal(false);
  readonly error = signal("");

  private identityKey(): string {
    const host = getRemoteHost();
    return host ? `${host.sessionVersion()}:${host.user()?.username ?? ""}` : "standalone";
  }

  /** Makes sure a DAF session exists for the current identity, once. */
  ensure(): void {
    const key = this.identityKey();
    if (this.loadedFor === key) return;
    this.loadedFor = key;
    if (this.tokens.isLoggedIn() && this.auth.user()) return;
    if (!getRemoteHost()) return; // standalone: the login page owns sign-in
    this.loading.set(true);
    this.error.set("");
    this.auth.attemptSso().subscribe({
      next: (user) => {
        this.loading.set(false);
        if (!user) {
          this.loadedFor = null;
          this.error.set(
            "DAF could not sign you in with Windows SSO from this browser. Open Committee Agendas on its own to sign in with your credentials.",
          );
        }
      },
      error: () => {
        this.loading.set(false);
        this.loadedFor = null;
        this.error.set("The DAF service did not answer. It may be down or unreachable from this network.");
      },
    });
  }

  /** Forget the failure so `ensure()` tries again (retry buttons). */
  retry(): void {
    this.auth.markSsoFailed(false);
    this.loadedFor = null;
    this.ensure();
  }
}
