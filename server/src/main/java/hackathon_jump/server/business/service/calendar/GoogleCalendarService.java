package hackathon_jump.server.business.service.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event as GoogleEvent;
import com.google.api.services.calendar.model.Events;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "MeetScribe Server";
    private static final NetHttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize HTTP transport", e);
        }
    }

    public List<GoogleEvent> getCalendarEvents(String accessToken) {
        try {
            Credential credential = new GoogleCredential()
                    .setAccessToken(accessToken);

            Calendar service = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Get all calendars
            CalendarList calendarList = service.calendarList().list().execute();
            List<CalendarListEntry> calendars = calendarList.getItems();
            
            List<GoogleEvent> allEvents = new ArrayList<>();
            
            // Fetch events from each calendar
            for (CalendarListEntry calendar : calendars) {
                try {
                    Events events = service.events()
                            .list(calendar.getId())
                            .setOrderBy("startTime")
                            .setSingleEvents(true)
                            .execute();

                    List<GoogleEvent> googleEvents = events.getItems();
                    if (googleEvents != null) {
                        allEvents.addAll(googleEvents);
                        log.info("Retrieved {} events from calendar: {}", googleEvents.size(), calendar.getSummary());
                    }
                } catch (IOException e) {
                    log.warn("Failed to retrieve events from calendar: {}", calendar.getSummary(), e);
                }
            }
            
            log.info("Retrieved {} total calendar events from {} calendars", allEvents.size(), calendars.size());
            return allEvents;

        } catch (IOException e) {
            log.error("Error retrieving calendar events", e);
            return Collections.emptyList();
        }
    }
}
