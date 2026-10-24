package dev.incidentiq.api;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component
public class Maintenance {private final IncidentService app;public Maintenance(IncidentService a){app=a;}@Scheduled(fixedDelay=60000) public void expire(){app.expireRca();}}
