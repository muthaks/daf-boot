/**
 * DAF (approvalforum) sign-in contracts. DAF issues its own tokens: the SSO server describes the
 * Windows user, DAF turns that description into a role token (docs/AUTHENTICATION.md).
 */

/** The SSO server's description of the signed-in Windows user (`GET <sso server>userdetail`). */
export interface WindowsAuthDetail {
  userPrincipalName?: string;
  samAccountName?: string;
  username?: string;
  displayName?: string;
  givenName?: string;
  surname?: string;
  emailAddress?: string;
  domain?: string;
  /** Domain the SSO server itself runs in; DAF looks the user up in AD when it differs from rmb.co.za. */
  hostDomain?: string;
  groups?: string[];
  [key: string]: unknown;
}

/** `POST <sso server>loginpost` (credentials checked by the SSO server). */
export interface SsoCredentialResponse {
  isValid: boolean;
  userDetail?: WindowsAuthDetail | null;
  /** Why the SSO server refused the credentials, e.g. "bind failed: Invalid Credentials". */
  errorMessage?: string;
}

/** `POST /auth/ssoToken` on DAF: the role token the agenda API accepts as bearer. */
export interface DafTokenResponse {
  Token: string;
  UserPrincipalName?: string;
}

/** `POST /auth/tenant` on DAF. */
export interface DafTenantResponse {
  tenantId: number;
}

/** The signed-in user as this app keeps it (decoded from the DAF token + the SSO detail). */
export interface DafUser {
  username: string;
  displayName: string;
  domain: string;
  /** DAF role strings from the token, e.g. creditanalyst, chairperson, approver, reviewanalyst. */
  roles: string[];
  tenantId: number | null;
}
