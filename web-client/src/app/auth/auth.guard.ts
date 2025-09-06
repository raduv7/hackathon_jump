import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const token = localStorage.getItem('token');
    
    if (token) {
      return true;
    } else {
      // Store the attempted URL for redirecting after login
      this.router.navigate(['/login'], { 
        queryParams: { returnUrl: state.url } 
      });
      return false;
    }
  }
}
