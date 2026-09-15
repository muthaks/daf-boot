import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { ActivatedRoute, Router } from "@angular/router";
import { CoLoginPageComponent } from "co-ds";
import { AuthService } from "../../core/services/auth.service";
import { PermissionsService } from "../../core/services/permissions.service";

/**
 * Standalone sign-in (never rendered when hosted — the shell signs the user in and the DAF
 * session is established silently). Same mechanism DAF's own UI uses: silent Windows SSO once per
 * tab, otherwise RMB credentials checked by the SSO server; either way DAF issues the role token.
 */
@Component({
  selector: "app-login",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CoLoginPageComponent],
  host: { "data-co-daf": "", "data-co-pattern": "daf-login" },
  templateUrl: "./login.component.html",
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly permissions = inject(PermissionsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly username = signal("");
  readonly password = signal("");
  readonly error = signal("");
  readonly loading = signal(false);
  readonly ssoLoading = signal(false);

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.returnToApplication();
      return;
    }
    // Silent SSO once per tab; a failed attempt or a sign-out sets the flag.
    if (!this.auth.ssoFailed()) this.attemptSso(true);
  }

  login(): void {
    if (this.loading()) return;
    this.error.set("");
    this.loading.set(true);
    this.auth
      .login(this.username().trim(), this.password())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.returnToApplication();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(
            err?.error?.reason ||
              err?.error?.message ||
              err?.message ||
              "Sign-in failed. Check your username and password.",
          );
        },
      });
  }

  loginWithSso(): void {
    this.auth.markSsoFailed(false);
    this.attemptSso(false);
  }

  private attemptSso(silent: boolean): void {
    if (this.ssoLoading()) return;
    this.error.set("");
    this.ssoLoading.set(true);
    this.auth
      .attemptSso()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user) => {
        this.ssoLoading.set(false);
        if (user) {
          this.returnToApplication();
          return;
        }
        if (!silent)
          this.error.set("Windows SSO is not available. Sign in with your credentials.");
      });
  }

  private returnToApplication(): void {
    if (!this.permissions.can("agenda.view")) {
      void this.router.navigateByUrl("/unauthorized");
      return;
    }
    const target = this.route.snapshot.queryParamMap.get("returnUrl") ?? "/home";
    void this.router.navigateByUrl(
      target.startsWith("/") &&
        !target.startsWith("//") &&
        !target.includes("\\") &&
        !target.startsWith("/login")
        ? target
        : "/home",
    );
  }
}
