import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE_URL } from '../security.config';
import { EventsService, Event } from '../services/events.service';
import { Subscription } from 'rxjs';

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
export class DashboardComponent implements OnInit, OnDestroy {
  userEmail: string = '';
  userName: string = '';
  userPicture: string = '';
  userProvider: string = '';
  
  events: Event[] = [];
  autoRefresh: boolean = false;
  notifications: boolean = true;
  
  private refreshInterval: any;
  private eventsSubscription: Subscription = new Subscription();

  constructor(
    private router: Router,
    private http: HttpClient,
    private eventsService: EventsService
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.subscribeToEvents();
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    this.eventsSubscription.unsubscribe();
  }

  subscribeToEvents() {
    this.eventsSubscription = this.eventsService.events$.subscribe(events => {
      this.events = events;
    });
  }

  private loadUserInfo() {
    this.userEmail = localStorage.getItem('user_email') || '';
    this.userName = localStorage.getItem('user_name') || '';
    this.userPicture = localStorage.getItem('user_picture') || '';
    this.userProvider = localStorage.getItem('user_provider') || '';
  }


  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  loadCalendarEvents() {
    this.eventsService.loadEvents().subscribe({
      next: (events) => {
        console.log('Calendar events loaded:', events);
        this.eventsService.updateEvents(events);
      },
      error: (error) => {
        console.error('Error loading calendar events:', error);
        // Handle error - maybe show a toast notification
      }
    });
  }

  formatEventTime(startDateTime: string): string {
    const date = new Date(startDateTime);
    return date.toLocaleString();
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
}
