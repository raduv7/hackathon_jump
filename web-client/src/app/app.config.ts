import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';

import { routes } from './app.routes';
import { API_BASE_URL } from './security.config';

// Logout function that matches the app component's performLogout logic
function performLogout() {
  // Clear all stored data (matching app.component.ts logic)
  localStorage.removeItem('token');
  localStorage.removeItem('user_email');
  localStorage.removeItem('user_name');
  localStorage.removeItem('user_picture');
  localStorage.removeItem('user_provider');
  
  // Clear social account data
  localStorage.removeItem('facebookUsername');
  localStorage.removeItem('facebookName');
  localStorage.removeItem('linkedinUsername');
  localStorage.removeItem('linkedinName');
  localStorage.removeItem('linkedin_account');
  
  // Clear Google account data
  localStorage.removeItem('googleEmailList');
  localStorage.removeItem('googleNameList');
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([
      (req, next) => {
        const router = inject(Router);
        const isAbsolute = /^https?:\/\//i.test(req.url);
        const url = isAbsolute ? req.url : `${API_BASE_URL}${req.url}`;

        // Add Authorization header if token exists in localStorage
        const token = localStorage.getItem('token');
        let headers = req.headers;
        if (token) {
          headers = headers.set('Authorization', `Bearer ${token}`);
        }

        return next(req.clone({ url, headers })).pipe(
          catchError((error) => {
            if (error.status === 401) {
              // Clear the token from localStorage
              performLogout();

              // Show message and redirect to login
              alert('Your session expired, you have to log in again.');
              router.navigate(['/login']);
            }
            throw error;
          })
        );
      }
    ]))
  ]
};
