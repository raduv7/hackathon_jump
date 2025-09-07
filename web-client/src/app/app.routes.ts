import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { CallbackComponent } from './auth/callback/callback.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { FutureEventsComponent } from './future-events/future-events.component';
import { PastEventsComponent } from './past-events/past-events.component';
import { AutomationsComponent } from './automations/automations.component';
import { EventDetailComponent } from './event-detail/event-detail.component';
import { AuthGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'settings' },
  { path: 'login', component: LoginComponent },
  { path: 'auth/oauth2/callback', component: CallbackComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'future-events', component: FutureEventsComponent, canActivate: [AuthGuard] },
  { path: 'past-events', component: PastEventsComponent, canActivate: [AuthGuard] },
  { path: 'event-detail/:id', component: EventDetailComponent, canActivate: [AuthGuard] },
  { path: 'automations', component: AutomationsComponent, canActivate: [AuthGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'login' }
];
