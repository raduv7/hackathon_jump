import { Component, OnInit } from '@angular/core';
import { RouterOutlet, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE_URL } from './security.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'web-client';
  
  isAuthenticated: boolean = false;
  isLoginPage: boolean = false;
  userEmail: string = '';
  userName: string = '';
  userPicture: string = '';
  userProvider: string = '';
  
  showLogoutDialog: boolean = false;
  isLoggingOut: boolean = false;

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.checkAuthentication();
    this.loadUserInfo();
    this.checkCurrentRoute();
    
    // Subscribe to route changes
    this.router.events.subscribe(() => {
      this.checkCurrentRoute();
      this.checkAuthentication(); // Re-check authentication on route changes
      this.loadUserInfo(); // Re-load user info on route changes
    });
  }

  private checkAuthentication() {
    const token = localStorage.getItem('token');
    this.isAuthenticated = !!token;
  }

  private loadUserInfo() {
    this.userEmail = localStorage.getItem('user_email') || '';
    this.userName = localStorage.getItem('user_name') || '';
    this.userPicture = localStorage.getItem('user_picture') || '';
    this.userProvider = localStorage.getItem('user_provider') || '';
  }

  private checkCurrentRoute() {
    this.isLoginPage = this.router.url === '/login' || this.router.url === '/';
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // Navigation methods
  goToSettings() {
    this.router.navigate(['/settings']);
  }

  // Logout methods
  confirmLogout() {
    this.showLogoutDialog = true;
  }

  cancelLogout() {
    this.showLogoutDialog = false;
  }

  logout() {
    this.isLoggingOut = true;
    const token = localStorage.getItem('token');
    
    if (token) {
      // Call sign_out API
      const headers = this.getAuthHeaders();
      this.http.post(`${API_BASE_URL}/auth/sign_out`, {}, { headers })
        .subscribe({
          next: () => {
            this.performLogout();
          },
          error: (error) => {
            console.error('Error signing out:', error);
            // Still perform logout even if API call fails
            this.performLogout();
          }
        });
    } else {
      this.performLogout();
    }
  }

  private performLogout() {
    // Clear all stored data
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
    
    // Close dialog
    this.showLogoutDialog = false;
    this.isLoggingOut = false;
    this.isAuthenticated = false;
    
    // Redirect to login
    this.router.navigate(['/login']);
  }
}
