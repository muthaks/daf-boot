import type { HttpClient } from "@angular/common/http";
import { signal } from "@angular/core";

/** Structural host contract: these are the shell's instances, not another auth bootstrap. */
export interface DafRemoteHost {
  http: HttpClient;
  basePath: string;
  user: () => { username: string; displayName?: string } | null;
  sessionVersion: () => number;
  signIn: (returnUrl: string) => void;
}

let host: DafRemoteHost | undefined;

/**
 * The permissions of the identity currently signed in to the shell, published by
 * `PermissionsService` once the DAF session is known. Kept with the identity they were
 * computed for, so a shell session change never shows the previous user's menu.
 */
const access = signal<{
  identity: string;
  permissions: Readonly<Record<string, boolean>>;
} | null>(null);

export function configureRemoteHost(context: DafRemoteHost): void {
  host = context;
}

export function getRemoteHost(): DafRemoteHost | undefined {
  return host;
}

export function setRemoteAccess(
  identity: string,
  permissions: Readonly<Record<string, boolean>> | null,
): void {
  access.set(permissions ? { identity, permissions } : null);
}

/** Read by the shell's computed menu (through `getRemoteNavigation`); null until known for the current identity. */
export function getRemotePermissions(): Readonly<Record<string, boolean>> | null {
  const current = access();
  const identity = host && `${host.sessionVersion()}:${host.user()?.username ?? ""}`;
  return current && current.identity === identity ? current.permissions : null;
}
