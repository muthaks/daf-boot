import { computed, effect, inject, Injectable } from "@angular/core";
import { DafPermission, DafPermissions, derivePermissions } from "../models/permissions";
import { getRemoteHost, setRemoteAccess } from "../../remote/remote-host";
import { SessionService } from "./session.service";

/**
 * Signal-based permission store for the UI.
 *
 * `permissions()` is computed from the DAF session, so every button, menu item and page that
 * reads it updates the moment the session is established, changes identity (hosted) or is
 * cleared (sign-out). Hosted in the shell, the same permissions are published to
 * `remote-host.ts` so the shell's menu (built by `getRemoteNavigation`) filters items the same way.
 *
 * Use in templates through the directives in `shared/directives`:
 * `*dafCan="'agenda.book'"` hides, `[dafDisableUnless]="'agenda.book'"` disables.
 */
@Injectable({ providedIn: "root" })
export class PermissionsService {
  private readonly session = inject(SessionService);

  /** Every permission, false until the session is known. */
  readonly permissions = computed<DafPermissions>(() =>
    derivePermissions(this.session.session()),
  );

  /** True once DAF has issued a token, so "no permission" can be told apart from "still loading". */
  readonly known = computed(() => this.session.session() !== null);

  constructor() {
    effect(() => {
      const host = getRemoteHost();
      if (!host) return;
      const identity = `${host.sessionVersion()}:${host.user()?.username ?? ""}`;
      setRemoteAccess(identity, this.known() ? this.permissions() : null);
    });
  }

  can(permission: DafPermission): boolean {
    return this.permissions()[permission];
  }
}
