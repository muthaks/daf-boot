import { inject, Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { getRemoteHost } from "../../remote/remote-host";

/** Route helpers that respect the shell's base path when hosted. */
@Injectable({ providedIn: "root" })
export class DafNavigationService {
  private readonly router = inject(Router);
  href(path: string): string {
    return `${getRemoteHost()?.basePath ?? ""}${path}`;
  }
  navigate(path: string): void {
    void this.router.navigateByUrl(this.href(path));
  }
}
