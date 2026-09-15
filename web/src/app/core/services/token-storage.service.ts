import { Injectable } from "@angular/core";
import { DafUser } from "../models/auth.models";

const TOKEN_KEY = "daf.session.token";
const USER_KEY = "daf.session.user";

/**
 * The DAF session for this tab: the DAF role token and the user it names. The same store serves
 * standalone and hosted runs, because DAF only accepts its own token (docs/AUTHENTICATION.md).
 */
@Injectable({ providedIn: "root" })
export class TokenStorageService {
  saveSession(token: string, user: DafUser): void {
    try {
      sessionStorage.setItem(TOKEN_KEY, token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {
      /* storage unavailable — session stays in memory for this navigation only */
    }
  }

  getToken(): string | null {
    try {
      const token = sessionStorage.getItem(TOKEN_KEY);
      return token && tokenExpiry(token) > Date.now() ? token : null;
    } catch {
      return null;
    }
  }

  getUser(): DafUser | null {
    try {
      const raw = sessionStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as DafUser) : null;
    } catch {
      return null;
    }
  }

  clear(): void {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    } catch {
      /* nothing to clear */
    }
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }
}

function claims(token: string): Record<string, unknown> {
  const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(
    atob(payload.padEnd(payload.length + ((4 - (payload.length % 4)) % 4), "=")),
  ) as Record<string, unknown>;
}

/** `exp` claim of a JWT in milliseconds; 0 when unreadable. */
export function tokenExpiry(token: string): number {
  try {
    const exp = claims(token)["exp"];
    return typeof exp === "number" ? exp * 1000 : 0;
  } catch {
    return 0;
  }
}

/** The claims DAF stamps on its role token: `sub` (user principal name) and `roles`. */
export function tokenIdentity(token: string): { username: string; roles: string[] } | null {
  try {
    const c = claims(token);
    if (typeof c["sub"] !== "string") return null;
    const roles = c["roles"];
    return {
      username: c["sub"],
      roles: Array.isArray(roles)
        ? roles.filter((r: unknown): r is string => typeof r === "string")
        : typeof roles === "string"
          ? roles.split(",").map((r) => r.trim()).filter(Boolean)
          : [],
    };
  } catch {
    return null;
  }
}
