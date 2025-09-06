package hackathon_jump.server.model.enums;

public enum EMeetingPlatform {
    GOOGLE_MEET,
    ZOOM,
    TEAMS;
    
    public static EMeetingPlatform fromLink(String link) {
        if (link == null || link.isEmpty()) {
            return null;
        }
        
        String lowerLink = link.toLowerCase();
        
        if (lowerLink.contains("meet.google.com") || lowerLink.contains("google.com/meet")) {
            return GOOGLE_MEET;
        } else if (lowerLink.contains("zoom.us") || lowerLink.contains("zoom.com")) {
            return ZOOM;
        } else if (lowerLink.contains("teams.microsoft.com") || lowerLink.contains("teams.live.com")) {
            return TEAMS;
        } else {
            return null;
        }
    }
}
