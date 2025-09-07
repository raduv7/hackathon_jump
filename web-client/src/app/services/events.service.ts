import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { API_BASE_URL } from '../security.config';

export interface Event {
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
}

@Injectable({
  providedIn: 'root'
})
export class EventsService {
  private eventsSubject = new BehaviorSubject<Event[]>([]);
  public events$ = this.eventsSubject.asObservable();

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  loadEvents(): Observable<Event[]> {
    const headers = this.getAuthHeaders();

    return this.http.get<Event[]>(`${API_BASE_URL}/events`, { headers });
  }

  updateEvents(events: Event[]): void {
    this.eventsSubject.next(events);
  }

  updateShouldSendBot(eventId: number, shouldSendBot: boolean): Observable<any> {
    const headers = this.getAuthHeaders();

    return this.http.put(`${API_BASE_URL}/events/${eventId}/should_send_bot/${shouldSendBot}`, {}, { headers });
  }

  getEventsForDate(date: Date): Event[] {
    const events = this.eventsSubject.value;
    const targetDate = new Date(date);
    targetDate.setHours(0, 0, 0, 0);

    return events.filter(event => {
      const eventDate = new Date(event.startDateTime);
      eventDate.setHours(0, 0, 0, 0);
      return eventDate.getTime() === targetDate.getTime();
    });
  }

  getUpcomingEvents(): Event[] {
    const events = this.eventsSubject.value;

    return events.sort((a, b) => new Date(a.startDateTime).getTime() - new Date(b.startDateTime).getTime());
  }

  getCurrentUTCTime(): string {
    const now = new Date();
    const utcNow = new Date(now.getTime() + (now.getTimezoneOffset() * 60000));
    return utcNow.toISOString();
  }
}
