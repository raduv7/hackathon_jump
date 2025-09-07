import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EventsService, Event } from '../services/events.service';
import { API_BASE_URL } from '../security.config';
import { Subscription } from 'rxjs';

export interface EventReport {
  id: number;
  attendees: string;
  startDateTime: string;
  platform: string;
  transcript: string;
  emailText: string;
  postText: string;
  event: {
    id: number;
    title: string;
    description: string | null;
    startDateTime: string;
    link: string | null;
    location: string | null;
    creator: string;
    googleId: string;
    owner: {
      id: number;
      username: string;
      oauthToken: string;
      provider: string;
      minutesBeforeMeeting: number;
    };
    attendees: any[];
    sentBot: boolean;
    shouldSendBot: boolean;
  };
}

@Component({
  selector: 'app-past-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './past-events.component.html',
  styleUrls: ['./past-events.component.scss']
})
export class PastEventsComponent implements OnInit, OnDestroy {
  currentDate: Date = new Date();
  currentMonth: number = this.currentDate.getMonth();
  currentYear: number = this.currentDate.getFullYear();

  // Calendar data
  calendarDays: (Date | null)[] = [];
  monthNames: string[] = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  // Events data
  events: Event[] = [];
  eventReports: EventReport[] = [];
  pastEvents: EventReport[] = [];
  private eventsSubscription: Subscription = new Subscription();

  constructor(
    private http: HttpClient,
    private eventsService: EventsService,
    private router: Router
  ) { }

  ngOnInit() {
    this.generateCalendar();
    this.loadEventReports();
    this.subscribeToEvents();
  }

  ngOnDestroy() {
    this.eventsSubscription.unsubscribe();
  }

  subscribeToEvents() {
    this.eventsSubscription = this.eventsService.events$.subscribe(events => {
      this.events = events;
    });
  }

  loadEvents() {
    this.eventsService.loadEvents().subscribe({
      next: (response) => {
        console.log('Events response:', response);
        this.eventsService.updateEvents(response);
      },
      error: (error) => {
        console.error('Error loading events:', error);
      }
    });
  }

  loadEventReports() {
    const headers = this.getAuthHeaders();
    this.http.get<EventReport[]>(`${API_BASE_URL}/event_reports`, { headers }).subscribe({
      next: (response) => {
        console.log('Event reports response:', response);
        this.eventReports = response;
        this.pastEvents = this.getPastEventReports();
      },
      error: (error) => {
        console.error('Error loading event reports:', error);
      }
    });
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getPastEventReports(): EventReport[] {
    const now = new Date();
    return this.eventReports.filter(eventReport => {
      const eventDate = new Date(eventReport.startDateTime);
      return eventDate < now;
    }).sort((a, b) => new Date(b.startDateTime).getTime() - new Date(a.startDateTime).getTime());
  }

  generateCalendar() {
    this.calendarDays = [];

    // Get first day of the month and number of days
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    // Add empty cells for days before the first day of the month
    for (let i = 0; i < startingDayOfWeek; i++) {
      this.calendarDays.push(null);
    }

    // Add days of the month
    for (let day = 1; day <= daysInMonth; day++) {
      this.calendarDays.push(new Date(this.currentYear, this.currentMonth, day));
    }
  }

  previousMonth() {
    if (this.currentMonth === 0) {
      this.currentMonth = 11;
      this.currentYear--;
    } else {
      this.currentMonth--;
    }
    this.generateCalendar();
  }

  nextMonth() {
    if (this.currentMonth === 11) {
      this.currentMonth = 0;
      this.currentYear++;
    } else {
      this.currentMonth++;
    }
    this.generateCalendar();
  }

  goToToday() {
    this.currentDate = new Date();
    this.currentMonth = this.currentDate.getMonth();
    this.currentYear = this.currentDate.getFullYear();
    this.generateCalendar();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  }

  isFutureDate(date: Date): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date >= today;
  }

  getMonthYearString(): string {
    return `${this.monthNames[this.currentMonth]} ${this.currentYear}`;
  }

  // Get events for a specific date
  getEventsForDate(date: Date): Event[] {
    return this.eventsService.getEventsForDate(date);
  }

  // Get event reports for a specific date
  getEventReportsForDate(date: Date): EventReport[] {
    const targetDate = new Date(date);
    targetDate.setHours(0, 0, 0, 0);

    return this.eventReports.filter(eventReport => {
      const eventDate = new Date(eventReport.startDateTime);
      eventDate.setHours(0, 0, 0, 0);
      return eventDate.getTime() === targetDate.getTime();
    });
  }

  // Format event time for display (start time only)
  formatEventTime(startDateTime: string): string {
    const date = new Date(startDateTime);
    return date.toLocaleDateString() + ' at ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
  }

  // Check if event has empty link
  hasEmptyLink(event: Event): boolean {
    return !event.link || event.link.trim() === '';
  }

  // Placeholder method for future event functionality
  onDateClick(date: Date) {
    console.log('Date clicked:', date);
    const eventsForDate = this.getEventsForDate(date);
    console.log('Events for this date:', eventsForDate);
  }

  // Navigate to event detail page
  onEventClick(event: Event) {
    this.router.navigate(['/event-detail', event.id]);
  }

  // Navigate to event report detail page
  onEventReportClick(eventReport: EventReport) {
    this.router.navigate(['/event-detail', eventReport.id]);
  }

  // Get platform logo based on platform name
  getPlatformLogo(platform: string): string | null {
    if (!platform) return null;
    
    const platformLower = platform.toLowerCase();
    
    if (platformLower.includes('google') || platformLower.includes('meet')) {
      return 'assets/img/googleMeet.png';
    } else if (platformLower.includes('zoom')) {
      return 'assets/img/zoom.png';
    } else if (platformLower.includes('teams') || platformLower.includes('microsoft')) {
      return 'assets/img/teams.png';
    }
    
    return null;
  }
}
