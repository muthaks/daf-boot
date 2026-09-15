import {
  CoAppShellComponent,
  CoNavigationItem,
  CoPlatformFooterComponent,
  CoUserMenuItem,
} from "co-ds";
import { Component, computed, inject, OnDestroy, signal } from "@angular/core";
import { NavigationEnd, Router, RouterOutlet } from "@angular/router";
import { filter } from "rxjs";
import { AuthService } from "../core/services/auth.service";
import { PermissionsService } from "../core/services/permissions.service";
import { SessionService } from "../core/services/session.service";
import { getRemoteNavigation } from "../remote/remote.navigation";

/**
 * Standalone chrome: co-ds app shell with the thin menu, the signed-in user and
 * sign-out. Federation mounts feature routes without this (the platform shell
 * provides chrome, user menu and sign-out there).
 */
@Component({
  selector: "app-shell",
  standalone: true,
  imports: [CoAppShellComponent, CoPlatformFooterComponent, RouterOutlet],
  templateUrl: "./app-shell.component.html",
})
export class AppShellComponent implements OnDestroy {
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);
  private readonly permissions = inject(PermissionsService);
  readonly auth = inject(AuthService);
  readonly activeItem = signal(this.router.url);
  private readonly routeSub = this.router.events
    .pipe(filter((event) => event instanceof NavigationEnd))
    .subscribe(() => this.activeItem.set(this.router.url));
  readonly navigation = computed(() =>
    getRemoteNavigation({
      basePath: "",
      settingsBasePath: "/settings",
      role: "",
      groups: this.session.session()?.roles ?? [],
      permissions: this.permissions.known() ? this.permissions.permissions() : undefined,
    }),
  );
  readonly userDetail = computed(() => this.session.roleLabel());
  navigate(item: CoNavigationItem): void {
    if (item.id.startsWith("/")) this.router.navigateByUrl(item.id);
  }
  userAction(item: CoUserMenuItem): void {
    if (item.id === "sign-out") this.auth.logout();
  }
  ngOnDestroy(): void {
    this.routeSub.unsubscribe();
  }
}
