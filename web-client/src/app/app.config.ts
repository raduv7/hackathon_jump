import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { API_BASE_URL } from './security.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([
      (req, next) => {
        const isAbsolute = /^https?:\/\//i.test(req.url);
        const url = isAbsolute ? req.url : `${API_BASE_URL}${req.url}`;
        return next(req.clone({ url }));
      }
    ]))
  ]
};
