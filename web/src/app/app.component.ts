import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { CoLoadingIndicatorComponent } from "co-ds";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [RouterOutlet, CoLoadingIndicatorComponent],
  template: `<co-loading-indicator /><router-outlet></router-outlet>`,
})
export class AppComponent {
  title = "Committee Agendas";
}
