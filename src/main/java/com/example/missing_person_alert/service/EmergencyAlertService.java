package com.example.missing_person_alert.service;



import com.example.missing_person_alert.model.EmergencyAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmergencyAlertService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyAlertService.class);

    @Value("${dwd.cap.feed.url}")
    private String dwdCapFeedUrl;

    private List<EmergencyAlert> currentAlerts = new ArrayList<>();

    @Scheduled(fixedRate = 300000)
    public void fetchEmergencyAlerts() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String xmlResponse = restTemplate.getForObject(dwdCapFeedUrl, String.class);

            if (xmlResponse == null) {
                logger.warn("Received null response from DWD CAP feed URL: {}", dwdCapFeedUrl);
                return;
            }

            // Parse CAP XML
            List<EmergencyAlert> alerts = parseCapXml(xmlResponse);
            this.currentAlerts = alerts;
            logger.info("Fetched {} emergency alerts", alerts.size());
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("DWD CAP feed URL not found (404): {}. Please verify the URL.", dwdCapFeedUrl);
        } catch (Exception e) {
            logger.error("Failed to fetch emergency alerts: {}", e.getMessage(), e);
        }
    }

    private List<EmergencyAlert> parseCapXml(String xml) throws Exception {
        List<EmergencyAlert> alerts = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

        EmergencyAlert alert = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamReader.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("entry".equals(localName)) {
                    alert = new EmergencyAlert();
                } else if (alert != null) {
                    switch (localName) {
                        case "title":
                            alert.setHeadline(reader.getElementText());
                            break;
                        case "description":
                            alert.setDescription(reader.getElementText());
                            break;
                        case "areaDesc":
                            alert.setAreaDesc(reader.getElementText());
                            break;
                        case "severity":
                            alert.setSeverity(reader.getElementText());
                            break;
                    }
                }
            } else if (event == XMLStreamReader.END_ELEMENT && "entry".equals(reader.getLocalName())) {
                if (alert != null) {
                    alerts.add(alert);
                    alert = null;
                }
            }
        }
        reader.close();
        return alerts;
    }

    public List<EmergencyAlert> getCurrentAlerts() {
        return currentAlerts;
    }
}