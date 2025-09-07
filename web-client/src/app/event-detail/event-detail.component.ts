import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE_URL } from '../security.config';

interface EventDetail {
  title: string;
  startDateTime: string;
  endDateTime: string;
  description: string;
  location: string;
  link: string;
  creator: string;
  attendees: string;
  platform: string;
  transcript: string;
  emailText: string;
  postText: string;
  sentBot: boolean;
}

interface Automation {
  id: number;
  automationType: string;
  mediaPlatform: string;
  description: string;
  example: string;
}

interface EventReportAutomation {
  id: number;
  title: string;
  text: string;
  automation: Automation;
}

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss']
})
export class EventDetailComponent implements OnInit {
  eventId: string = '';
  eventDetail: EventDetail | null = null;
  automations: Automation[] = [];
  selectedAutomation: EventReportAutomation | null = null;
  showAutomationPopup: boolean = false;
  loading: boolean = false;
  error: string = '';


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.eventId = params['id'];
      if (this.eventId) {
        this.loadEventDetail();
        this.loadAutomations();
      }
    });
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  loadEventDetail() {
    this.loading = true;
    this.error = '';

    const headers = this.getAuthHeaders();
    this.http.get<EventDetail>(`${API_BASE_URL}/event_reports/${this.eventId}`, { headers }).subscribe({
      next: (event) => {
        this.eventDetail = event;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading event detail:', error);
        this.error = 'Failed to load event details';
        this.loading = false;
      }
    });
  }

  loadAutomations() {
    const headers = this.getAuthHeaders();
    this.http.get<Automation[]>(`${API_BASE_URL}/automations`, { headers }).subscribe({
      next: (automations) => {
        this.automations = automations;
      },
      error: (error) => {
        console.error('Error loading automations:', error);
      }
    });
  }

  onAutomationClick(automation: Automation) {
    if (!this.eventDetail) return;

    this.loading = true;
    const headers = this.getAuthHeaders();

    // Get event report ID from the event detail
    const eventReportId = this.getEventReportId();
    if (!eventReportId) {
      this.error = 'Event report not found';
      this.loading = false;
      return;
    }

    this.http.get<EventReportAutomation>(
      `${API_BASE_URL}/event-report-automations/automation/${automation.id}/event-report/${eventReportId}`,
      { headers }
    ).subscribe({
      next: (eventReportAutomation) => {
        this.selectedAutomation = eventReportAutomation;
        this.showAutomationPopup = true;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading automation data:', error);
        this.error = 'Failed to load automation data';
        this.loading = false;
      }
    });
  }

  private getEventReportId(): string | null {
    // This would need to be implemented based on your backend API
    // For now, we'll assume the event ID is the same as the event report ID
    return this.eventId;
  }

  closeAutomationPopup() {
    this.showAutomationPopup = false;
    this.selectedAutomation = null;
  }

  copyText() {
    if (this.selectedAutomation?.text) {
      navigator.clipboard.writeText(this.selectedAutomation.text).then(() => {
        // You could add a toast notification here
        console.log('Text copied to clipboard');
      }).catch(err => {
        console.error('Failed to copy text: ', err);
      });
    }
  }

  regenerateAutomation() {
    if (!this.selectedAutomation) return;

    this.loading = true;
    const headers = this.getAuthHeaders();

    this.http.put<EventReportAutomation>(
      `${API_BASE_URL}/event-report-automations/${this.selectedAutomation.id}/refresh`,
      {},
      { headers }
    ).subscribe({
      next: (updatedAutomation) => {
        this.selectedAutomation = updatedAutomation;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error regenerating automation:', error);
        this.error = 'Failed to regenerate automation';
        this.loading = false;
      }
    });
  }

  postToLinkedIn() {
    if (!this.selectedAutomation) return;

    this.loading = true;
    const headers = this.getAuthHeaders();

    this.http.post<{message: string, title: string, textLength: string}>(
      `${API_BASE_URL}/event-report-automations/${this.selectedAutomation.id}/linkedin`,
      {},
      { headers }
    ).subscribe({
      next: (response) => {
        console.log('Posted to LinkedIn:', response);
        this.loading = false;
        // You could add a success notification here
      },
      error: (error) => {
        console.error('Error posting to LinkedIn:', error);
        this.error = 'Failed to post to LinkedIn';
        this.loading = false;
      }
    });
  }

  isLinkedInAutomation(): boolean {
    return this.selectedAutomation?.automation.mediaPlatform === 'LINKEDIN';
  }

  goBack() {
    this.router.navigate(['/past-events']);
  }

  formatDateTime(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString() + ' at ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
  }

  getAutomationTypeDisplay(type: string): string {
    switch (type) {
      case 'POST': return 'Post';
      case 'MARKETING_CONTENT': return 'Marketing Content';
      case 'RECRUITING_CONTENT': return 'Recruiting Content';
      default: return type;
    }
  }

  getPlatformDisplay(platform: string): string {
    switch (platform) {
      case 'EMAIL': return 'Email';
      case 'FACEBOOK': return 'Facebook';
      case 'LINKEDIN': return 'LinkedIn';
      default: return platform;
    }
  }

  getSelectedAutomationType(): string {
    return this.selectedAutomation?.automation?.automationType || '';
  }

  getSelectedAutomationPlatform(): string {
    return this.selectedAutomation?.automation?.mediaPlatform || '';
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
