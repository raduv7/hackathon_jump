package hackathon_jump.server.business.service.external;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
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

    public List<Event> getCalendarEvents(String accessToken) throws IOException {
        Credential credential = new GoogleCredential()
                .setAccessToken(accessToken);

        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Get all calendars
        CalendarList calendarList = service.calendarList().list().execute();
        List<CalendarListEntry> calendars = calendarList.getItems();

        List<Event> allEvents = new ArrayList<>();

        // Fetch events from each calendar
        for (CalendarListEntry calendar : calendars) {
            // Get current time in RFC3339 format for Google Calendar API
            String currentTime = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            
            Events events = service.events()
                    .list(calendar.getId())
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMin(new com.google.api.client.util.DateTime(currentTime))
                    .execute();

            List<Event> googleEvents = events.getItems();
            if (googleEvents != null) {
                allEvents.addAll(googleEvents);
                log.info("Retrieved {} events from calendar: {}", googleEvents.size(), calendar.getSummary());
            }
        }

        log.info("Retrieved {} total future calendar events from {} calendars", allEvents.size(), calendars.size());
        return allEvents;
    }
}
