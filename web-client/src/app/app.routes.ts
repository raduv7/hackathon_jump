import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { CallbackComponent } from './auth/callback/callback.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'auth/oauth2/callback', component: CallbackComponent },
  { path: '**', redirectTo: 'login' }
];
