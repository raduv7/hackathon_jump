import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { CallbackComponent } from './auth/callback/callback.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { AuthGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'settings' },
  { path: 'login', component: LoginComponent },
  { path: 'auth/oauth2/callback', component: CallbackComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'login' }
];
