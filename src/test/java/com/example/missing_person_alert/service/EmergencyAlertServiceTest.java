package com.example.missing_person_alert.service;

import com.example.missing_person_alert.model.EmergencyAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class EmergencyAlertServiceTest {

    @InjectMocks
    private EmergencyAlertService emergencyAlertService;

    @BeforeEach
    void setUp() {
        // Set the DWD CAP feed URL via reflection
        setField(emergencyAlertService, "dwdCapFeedUrl", "https://www.dwd.de/DWD/warnungen/warnapp_rss/warnings_rss.xml");
    }

    // Helper method to set private field for testing
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    void parsesCapXmlSuccessfully() throws Exception {
        // Arrange
        String mockXmlResponse = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed>
                    <entry>
                        <title>Storm Warning</title>
                        <description>Heavy storm expected</description>
                        <areaDesc>Berlin</areaDesc>
                        <severity>Moderate</severity>
                    </entry>
                    <entry>
                        <title>Flood Warning</title>
                        <description>Possible flooding in coastal areas</description>
                        <areaDesc>Hamburg</areaDesc>
                        <severity>Severe</severity>
                    </entry>
                </feed>
                """;

        // Use reflection to access the private parseCapXml method
        Method parseCapXmlMethod = EmergencyAlertService.class.getDeclaredMethod("parseCapXml", String.class);
        parseCapXmlMethod.setAccessible(true);

        // Act
        List<EmergencyAlert> alerts = (List<EmergencyAlert>) parseCapXmlMethod.invoke(emergencyAlertService, mockXmlResponse);

        // Assert
        assertNotNull(alerts, "Parsed alerts list should not be null");
        assertEquals(2, alerts.size(), "Should have parsed 2 alerts");

        EmergencyAlert firstAlert = alerts.get(0);
        assertEquals("Storm Warning", firstAlert.getHeadline());
        assertEquals("Heavy storm expected", firstAlert.getDescription());
        assertEquals("Berlin", firstAlert.getAreaDesc());
        assertEquals("Moderate", firstAlert.getSeverity());

        EmergencyAlert secondAlert = alerts.get(1);
        assertEquals("Flood Warning", secondAlert.getHeadline());
        assertEquals("Possible flooding in coastal areas", secondAlert.getDescription());
        assertEquals("Hamburg", secondAlert.getAreaDesc());
        assertEquals("Severe", secondAlert.getSeverity());
    }

    @Test
    void handlesEmptyCapXml() throws Exception {
        // Arrange
        String emptyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed></feed>";

        // Use reflection to access the private parseCapXml method
        Method parseCapXmlMethod = EmergencyAlertService.class.getDeclaredMethod("parseCapXml", String.class);
        parseCapXmlMethod.setAccessible(true);

        // Act
        List<EmergencyAlert> alerts = (List<EmergencyAlert>) parseCapXmlMethod.invoke(emergencyAlertService, emptyXml);

        // Assert
        assertNotNull(alerts, "Parsed alerts list should not be null");
        assertTrue(alerts.isEmpty(), "Parsed alerts list should be empty for empty XML");
    }

    @Test
    void returnsEmptyAlertList() {
        // Act
        List<EmergencyAlert> alerts = emergencyAlertService.getCurrentAlerts();

        // Assert
        assertNotNull(alerts, "Alerts list should not be null");
        assertTrue(alerts.isEmpty(), "Alerts list should be empty initially");
    }

    @Test
    void setsAndRetrievesAlerts() throws Exception {
        // Arrange
        String mockXmlResponse = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed>
                    <entry>
                        <title>Storm Warning</title>
                        <description>Heavy storm expected</description>
                        <areaDesc>Berlin</areaDesc>
                        <severity>Moderate</severity>
                    </entry>
                </feed>
                """;

        // Use reflection to access the private parseCapXml method
        Method parseCapXmlMethod = EmergencyAlertService.class.getDeclaredMethod("parseCapXml", String.class);
        parseCapXmlMethod.setAccessible(true);
        List<EmergencyAlert> parsedAlerts = (List<EmergencyAlert>) parseCapXmlMethod.invoke(emergencyAlertService, mockXmlResponse);

        // Use reflection to set the private currentAlerts field
        setField(emergencyAlertService, "currentAlerts", parsedAlerts);

        // Act
        List<EmergencyAlert> alerts = emergencyAlertService.getCurrentAlerts();

        // Assert
        assertNotNull(alerts, "Alerts list should not be null");
        assertEquals(1, alerts.size(), "Should have 1 alert");
        EmergencyAlert alert = alerts.get(0);
        assertEquals("Storm Warning", alert.getHeadline());
        assertEquals("Heavy storm expected", alert.getDescription());
        assertEquals("Berlin", alert.getAreaDesc());
        assertEquals("Moderate", alert.getSeverity());
    }
}