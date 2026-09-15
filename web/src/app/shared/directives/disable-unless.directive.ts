import { computed, Directive, inject, input } from "@angular/core";
import { DafPermission } from "../../core/models/permissions";
import { PermissionsService } from "../../core/services/permissions.service";

/**
 * Disables its host control unless the user holds the permission, keeping the control visible
 * so the user can see the action exists:
 * `<button co-button [dafDisableUnless]="'agenda.manage'">Lock agenda</button>`.
 */
@Directive({
  selector: "[dafDisableUnless]",
  standalone: true,
  host: { "[attr.disabled]": "disabled() ? '' : null", "[attr.aria-disabled]": "disabled()" },
})
export class DisableUnlessDirective {
  readonly dafDisableUnless = input.required<DafPermission>();
  private readonly permissions = inject(PermissionsService);
  readonly disabled = computed(() => !this.permissions.can(this.dafDisableUnless()));
}
