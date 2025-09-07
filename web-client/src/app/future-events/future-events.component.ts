import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EventsService, Event } from '../services/events.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-future-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './future-events.component.html',
  styleUrls: ['./future-events.component.scss']
})
export class FutureEventsComponent implements OnInit, OnDestroy {
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
  futureEvents: Event[] = [];
  private eventsSubscription: Subscription = new Subscription();

  constructor(
    private http: HttpClient,
    private eventsService: EventsService
  ) { }

  ngOnInit() {
    this.generateCalendar();
    this.loadEvents();
    this.subscribeToEvents();
  }

  ngOnDestroy() {
    this.eventsSubscription.unsubscribe();
  }

  subscribeToEvents() {
    this.eventsSubscription = this.eventsService.events$.subscribe(events => {
      this.events = events;
      this.futureEvents = this.eventsService.getUpcomingEvents();
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
    const today = new Date();
    const utcToday = new Date(today.getTime() + (today.getTimezoneOffset() * 60000));
    this.currentDate = utcToday;
    this.currentMonth = utcToday.getMonth();
    this.currentYear = utcToday.getFullYear();
    this.generateCalendar();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    const utcToday = new Date(today.getTime() + (today.getTimezoneOffset() * 60000));
    return date.getDate() === utcToday.getDate() &&
           date.getMonth() === utcToday.getMonth() &&
           date.getFullYear() === utcToday.getFullYear();
  }

  isFutureDate(date: Date): boolean {
    const today = new Date();
    const utcToday = new Date(today.getTime() + (today.getTimezoneOffset() * 60000));
    utcToday.setHours(0, 0, 0, 0);
    return date >= utcToday;
  }

  getMonthYearString(): string {
    return `${this.monthNames[this.currentMonth]} ${this.currentYear}`;
  }

  // Get events for a specific date
  getEventsForDate(date: Date): Event[] {
    return this.eventsService.getEventsForDate(date);
  }

  // Format event time for display
  formatEventTime(startDateTime: string): string {
    const date = new Date(startDateTime);
    return date.toLocaleString();
  }

  // Update shouldSendBot status
  updateShouldSendBot(event: Event) {
    event.shouldSendBot = !event.shouldSendBot;
    this.eventsService.updateShouldSendBot(event.id, event.shouldSendBot).subscribe({
      next: () => {
        // Update the event in the local array
        const eventIndex = this.events.findIndex(e => e.id === event.id);
        if (eventIndex !== -1) {
          this.events[eventIndex].shouldSendBot = event.shouldSendBot;
          this.eventsService.updateEvents(this.events);
        }
      },
      error: (error) => {
        console.error('Error updating shouldSendBot:', error);
        // Revert the checkbox state on error
        event.shouldSendBot = !event.shouldSendBot;
      }
    });
  }

  // Check if event has empty link
  hasEmptyLink(event: Event): boolean {
    return !event.link || event.link.trim() === '';
  }

  // Get current UTC time for display
  getCurrentUTCTime(): string {
    return this.eventsService.getCurrentUTCTime();
  }

  // Placeholder method for future event functionality
  onDateClick(date: Date) {
    console.log('Date clicked:', date);
    const eventsForDate = this.getEventsForDate(date);
    console.log('Events for this date:', eventsForDate);
  }
}
