import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE_URL } from '../security.config';

interface CalendarEvent {
  id: string;
  summary: string;
  start: {
    dateTime?: string;
    date?: string;
  };
  end: {
    dateTime?: string;
    date?: string;
  };
  location?: string;
  description?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  userEmail: string = '';
  userName: string = '';
  userPicture: string = '';
  userProvider: string = '';
  
  events: CalendarEvent[] = [];
  autoRefresh: boolean = false;
  notifications: boolean = true;
  
  showLogoutDialog: boolean = false;
  isLoggingOut: boolean = false;
  
  private refreshInterval: any;

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.checkAuthentication();
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  private loadUserInfo() {
    this.userEmail = localStorage.getItem('user_email') || '';
    this.userName = localStorage.getItem('user_name') || '';
    this.userPicture = localStorage.getItem('user_picture') || '';
    this.userProvider = localStorage.getItem('user_provider') || '';
  }

  private checkAuthentication() {
    const token = localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  loadCalendarEvents() {
    const headers = this.getAuthHeaders();
    
    this.http.get<CalendarEvent[]>(`${API_BASE_URL}/api/calendar/events`, { headers })
      .subscribe({
        next: (events) => {
          this.events = events;
          console.log('Calendar events loaded:', events);
        },
        error: (error) => {
          console.error('Error loading calendar events:', error);
          // Handle error - maybe show a toast notification
        }
      });
  }

  formatEventTime(start: CalendarEvent['start']): string {
    if (start.dateTime) {
      const date = new Date(start.dateTime);
      return date.toLocaleString();
    } else if (start.date) {
      const date = new Date(start.date);
      return date.toLocaleDateString();
    }
    return 'No time specified';
  }

  toggleAutoRefresh() {
    if (this.autoRefresh) {
      // Start auto-refresh every 5 minutes
      this.refreshInterval = setInterval(() => {
        this.loadCalendarEvents();
      }, 5 * 60 * 1000);
    } else {
      // Stop auto-refresh
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
        this.refreshInterval = null;
      }
    }
  }

  toggleNotifications() {
    // Implement notification toggle logic
    console.log('Notifications toggled:', this.notifications);
  }

  goToSettings() {
    this.router.navigate(['/settings']);
  }

  goToAccounts() {
    this.router.navigate(['/accounts']);
  }

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
    
    // Stop any running intervals
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    
    // Close dialog
    this.showLogoutDialog = false;
    this.isLoggingOut = false;
    
    // Redirect to empty path (root)
    this.router.navigate(['/']);
  }
}
