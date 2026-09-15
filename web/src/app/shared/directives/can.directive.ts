import { Directive, effect, inject, input, TemplateRef, ViewContainerRef } from "@angular/core";
import { DafPermission } from "../../core/models/permissions";
import { PermissionsService } from "../../core/services/permissions.service";

/**
 * Renders its host only while the user holds the permission:
 * `<button *dafCan="'agenda.book'">Book agenda item</button>`.
 * Signal-driven: the element appears or disappears as the session loads or changes.
 */
@Directive({ selector: "[dafCan]", standalone: true })
export class CanDirective {
  readonly dafCan = input.required<DafPermission>();
  private readonly permissions = inject(PermissionsService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly container = inject(ViewContainerRef);
  private rendered = false;

  constructor() {
    effect(() => {
      const allowed = this.permissions.can(this.dafCan());
      if (allowed && !this.rendered) {
        this.container.createEmbeddedView(this.template);
        this.rendered = true;
      } else if (!allowed && this.rendered) {
        this.container.clear();
        this.rendered = false;
      }
    });
  }
}
