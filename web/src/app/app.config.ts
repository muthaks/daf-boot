import { ApplicationConfig, provideZoneChangeDetection } from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAnimationsAsync } from "@angular/platform-browser/animations/async";
import { coLoadingInterceptor, provideCoDs } from "co-ds";

import { routes } from "./app.routes";
import { errorInterceptor } from "./core/interceptors/error.interceptor";

/**
 * Standalone bootstrap only. Hosted in the Credit shell the routes are mounted in the shell's
 * injector, so its HttpClient and interceptors apply; the DAF token is attached by
 * `DafHttpClient` in both cases (DAF is not an auth-service audience, see docs/AUTHENTICATION.md).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideCoDs(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor, coLoadingInterceptor])),
    provideAnimationsAsync(),
  ],
};
