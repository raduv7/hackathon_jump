import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FutureEventsComponent } from './future-events.component';

describe('FutureEventsComponent', () => {
  let component: FutureEventsComponent;
  let fixture: ComponentFixture<FutureEventsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FutureEventsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FutureEventsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should generate calendar on init', () => {
    expect(component.calendarDays.length).toBeGreaterThan(0);
  });

  it('should navigate to previous month', () => {
    const initialMonth = component.currentMonth;
    component.previousMonth();
    expect(component.currentMonth).toBe(initialMonth === 0 ? 11 : initialMonth - 1);
  });

  it('should navigate to next month', () => {
    const initialMonth = component.currentMonth;
    component.nextMonth();
    expect(component.currentMonth).toBe(initialMonth === 11 ? 0 : initialMonth + 1);
  });

  it('should go to today', () => {
    const today = new Date();
    component.goToToday();
    expect(component.currentMonth).toBe(today.getMonth());
    expect(component.currentYear).toBe(today.getFullYear());
  });
});
