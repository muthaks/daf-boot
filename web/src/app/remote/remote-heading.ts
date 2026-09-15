import { createRemoteHeadingGuard } from "co-ds";

/**
 * This app's instance of the platform-standard heading guard (co-ds
 * `createRemoteHeadingGuard`). Every route in `remote.routes.ts` attaches it
 * so the shell renders the portal-style heading band for each page.
 */
export const withHeading = createRemoteHeadingGuard("Committee Agendas", "/home");
