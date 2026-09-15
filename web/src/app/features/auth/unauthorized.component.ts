import { Component, inject } from "@angular/core";
import { CoButtonComponent, CoDetailCardComponent, CoEmptyStateComponent } from "co-ds";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-unauthorized",
  standalone: true,
  host: { "data-co-daf": "", "data-co-pattern": "daf-unauthorized" },
  imports: [CoButtonComponent, CoDetailCardComponent, CoEmptyStateComponent],
  template: `
    <co-detail-card ariaLabel="Access denied">
      <co-empty-state
        title="You don't have an agenda role"
        hint="Committee agendas are open to DAF credit analysts, review analysts, approvers, chairpersons and the secretariat. Your DAF sign-in succeeded but carries none of these roles. Ask the DAF administrators for the right DAF group."
      />
      <button co-button (click)="auth.logout()">Sign out</button>
    </co-detail-card>
  `,
})
export class UnauthorizedComponent {
  readonly auth = inject(AuthService);
}
