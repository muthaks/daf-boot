import { Component, computed, inject, OnInit, signal } from "@angular/core";
import {
  CoFavouriteChange,
  CoPageHeadingStore,
  CoWorkspaceActivity,
  CoWorkspaceHomeComponent,
  CoWorkspaceInformation,
  CoWorkspaceLink,
  CoWorkspaceSystem,
} from "co-ds";
import { AuthService } from "../../core/services/auth.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";

const FAVOURITES_KEY = "daf.home.favourites";
const RECENTS_KEY = "daf.home.recents";
interface RecentView {
  id: string;
  openedAt: number;
}

/** Landing page on the platform-standard home layout (co-workspace-home). */
@Component({
  selector: "app-daf-home",
  standalone: true,
  imports: [CoWorkspaceHomeComponent],
  host: { "data-co-daf": "", "data-co-pattern": "daf-home" },
  template: `
    <co-workspace-home
      [name]="firstName()"
      productName="Committee Agendas"
      description="Book credit papers onto committee agendas, compile the running order and capture what the committee decided."
      searchLabel="Search Committee Agendas"
      searchPlaceholder="Search views by name…"
      workspaceTitle="Your workspace"
      workspaceDescription="Your favourite and recently opened views."
      searchResultsDescription="Views matching your search."
      loadingMessage="Signing you in to DAF…"
      workspaceEmptyMessage="Views appear here once DAF has signed you in."
      searchEmptyMessage="No view matches your search."
      unavailableMessage="Retry to sign in to DAF."
      activityDescription="Views you have opened in this browser."
      activityEmptyMessage="Open a view to start your recent activity."
      activityTypeLabel="View"
      attentionTitle="Your access"
      [attentionEmptyMessage]="attentionMessage()"
      guidanceTitle="At a glance"
      [shortcuts]="shortcuts()"
      [overview]="overview()"
      [guidance]="overview()"
      [systems]="views()"
      [activity]="activity()"
      [attention]="attention()"
      [query]="query()"
      (queryChange)="query.set($event)"
      [loading]="session.loading()"
      [error]="session.error()"
      [workspaceAvailable]="!session.error()"
      (navigate)="open($event)"
      (favouriteChange)="setFavourite($event)"
      (retry)="session.retry()"
    />
  `,
})
export class DafHomeComponent implements OnInit {
  private readonly heading = inject(CoPageHeadingStore);
  private readonly auth = inject(AuthService);
  private readonly navigation = inject(DafNavigationService);
  private readonly permissions = inject(PermissionsService);
  readonly session = inject(SessionService);
  readonly query = signal("");
  readonly favourites = signal<string[]>(this.readList(FAVOURITES_KEY));
  readonly recents = signal<RecentView[]>(this.readRecents());

  constructor() {
    // The hero carries the welcome; no shared heading band on home.
    this.heading.clear();
  }

  private readonly catalog = computed(() => {
    const views = [
      {
        id: "/agenda",
        title: "All agenda items",
        description: "Every item booked on a committee agenda, with its meeting and status.",
        icon: "pi pi-list",
        permission: "agenda.view" as const,
      },
      {
        id: "/agenda/my",
        title: "My agenda items",
        description: "Items you booked or analyse.",
        icon: "pi pi-user",
        permission: "agenda.view" as const,
      },
      {
        id: "/agenda/new",
        title: "Book an agenda item",
        description: "Reserve a slot on a committee meeting for a credit paper.",
        icon: "pi pi-calendar-plus",
        permission: "agenda.book" as const,
      },
      {
        id: "/agenda/dashboard",
        title: "Committee dashboard",
        description: "Meeting schedule, running order and the committee report.",
        icon: "pi pi-calendar",
        permission: "agenda.view" as const,
      },
      {
        id: "/agenda/admin",
        title: "Committee administration",
        description: "Committees, cut-off rules, quorum roles and the meeting calendar.",
        icon: "pi pi-cog",
        permission: "agenda.admin" as const,
      },
    ];
    return views.filter((v) => this.permissions.can(v.permission));
  });

  readonly firstName = computed(() => this.auth.displayName().trim().split(/\s+/)[0] || "");

  readonly attentionMessage = computed(() => {
    if (!this.session.session()) return "Your access is being confirmed.";
    const label = this.session.roleLabel();
    if (label === "Chairperson")
      return "You are a chairperson: you can approve items, validate papers, compile and lock agendas.";
    if (label === "Secretary")
      return "You are the secretariat: you administer committees and the meeting calendar and manage meetings.";
    if (label === "Approver") return "You are an approver: you can review and approve submitted items.";
    if (label === "Review analyst")
      return "You are a review analyst: you can review submitted items and validate credit papers.";
    if (label === "Credit analyst")
      return "You are a credit analyst: you can book items, upload credit papers and submit them for review.";
    return "Your DAF account has no agenda role.";
  });

  readonly overview = computed<CoWorkspaceInformation[]>(() => {
    const s = this.session.session();
    return [
      {
        id: "role",
        title: s ? this.session.roleLabel() || "No agenda role" : "Access pending",
        description: s
          ? `Signed in to DAF as ${s.displayName || s.username}.`
          : "Your permissions are shown once DAF has signed you in.",
        icon: "pi pi-user",
      },
      {
        id: "roles",
        title: s ? `${s.roles.length} DAF role${s.roles.length === 1 ? "" : "s"}` : "– roles",
        description: s?.roles.length ? s.roles.join(", ") : "Roles from your DAF token.",
        icon: "pi pi-sitemap",
      },
    ];
  });

  readonly shortcuts = computed<CoWorkspaceLink[]>(() =>
    this.catalog().map((v) => ({ ...v, href: this.navigation.href(v.id) })),
  );

  readonly views = computed<CoWorkspaceSystem[]>(() => {
    const q = this.query().trim().toLowerCase();
    const favourites = this.favourites();
    return this.catalog()
      .filter((v) => !q || (v.title + " " + v.description).toLowerCase().includes(q))
      .map((v) => {
        const recent = this.recents().find((r) => r.id === v.id);
        return {
          ...v,
          href: this.navigation.href(v.id),
          favourite: favourites.includes(v.id),
          environment: "DAF",
          lastUsed: recent ? "Opened " + this.relativeTime(recent.openedAt) : "Not opened yet",
        };
      });
  });

  readonly activity = computed<CoWorkspaceActivity[]>(() =>
    this.recents().flatMap((recent) => {
      const view = this.catalog().find((v) => v.id === recent.id);
      return view
        ? [
            {
              id: view.id,
              title: view.title,
              href: this.navigation.href(view.id),
              time: this.relativeTime(recent.openedAt),
              dateTime: new Date(recent.openedAt).toISOString(),
              detail: view.description,
            },
          ]
        : [];
    }),
  );

  readonly attention = computed<CoWorkspaceLink[]>(() => {
    const s = this.session.session();
    if (!s || this.permissions.can("agenda.view")) return [];
    return [
      {
        id: "no-role",
        title: "No agenda role",
        description:
          "Your DAF account has none of the agenda roles. Ask the DAF administrators for access.",
        icon: "pi pi-info-circle",
        href: this.navigation.href("/unauthorized"),
      },
    ];
  });

  ngOnInit(): void {
    this.session.ensure();
  }

  open(id: string): void {
    if (!this.catalog().some((v) => v.id === id)) return;
    this.recents.update((recents) => {
      const next = [{ id, openedAt: Date.now() }, ...recents.filter((r) => r.id !== id)].slice(0, 5);
      this.write(RECENTS_KEY, next);
      return next;
    });
    this.navigation.navigate(id);
  }

  setFavourite(change: CoFavouriteChange): void {
    if (!this.catalog().some((v) => v.id === change.id)) return;
    this.favourites.update((f) => {
      const next = change.favourite
        ? [...new Set([...f, change.id])]
        : f.filter((id) => id !== change.id);
      this.write(FAVOURITES_KEY, next);
      return next;
    });
  }

  private readList(key: string): string[] {
    try {
      const value: unknown = JSON.parse(localStorage.getItem(key) || "[]");
      return Array.isArray(value) ? value.filter((id): id is string => typeof id === "string") : [];
    } catch {
      return [];
    }
  }

  private readRecents(): RecentView[] {
    try {
      const value: unknown = JSON.parse(localStorage.getItem(RECENTS_KEY) || "[]");
      return Array.isArray(value)
        ? value
            .filter(
              (item): item is RecentView =>
                !!item &&
                typeof item.id === "string" &&
                typeof item.openedAt === "number" &&
                Number.isFinite(item.openedAt) &&
                item.openedAt > 0 &&
                item.openedAt <= Date.now(),
            )
            .slice(0, 5)
        : [];
    } catch {
      return [];
    }
  }

  private write(key: string, value: unknown): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      /* In-memory only. */
    }
  }

  private relativeTime(timestamp: number): string {
    const minutes = Math.max(0, Math.floor((Date.now() - timestamp) / 60000));
    if (minutes < 1) return "just now";
    const relative = new Intl.RelativeTimeFormat("en", { numeric: "auto" });
    if (minutes < 60) return relative.format(-minutes, "minute");
    if (minutes < 1440) return relative.format(-Math.floor(minutes / 60), "hour");
    return relative.format(-Math.floor(minutes / 1440), "day");
  }
}
