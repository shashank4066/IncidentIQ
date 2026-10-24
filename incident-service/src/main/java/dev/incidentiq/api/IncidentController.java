package dev.incidentiq.api;
import dev.incidentiq.api.Dtos.*;
import dev.incidentiq.contracts.Contracts.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping("/api") @Validated
public class IncidentController {
 private final IncidentService app;private final org.springframework.core.env.Environment env;
 public IncidentController(IncidentService app,org.springframework.core.env.Environment env){this.app=app;this.env=env;}
 @GetMapping("/me") public Map<String,Object> me(Principal p){return Map.of("username",p.getName(),"demo",env.matchesProfiles("demo"));}
 @GetMapping("/incidents") public PageResult list(@RequestParam(required=false) @Size(max=200) String search,@RequestParam(required=false) Status status,@RequestParam(required=false) Severity severity,@RequestParam(required=false) String service,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return app.list(search,status,severity,service,page,size);}
 @PostMapping("/alerts") public ResponseEntity<AlertResult> ingest(@RequestBody @Valid AlertInput a,Principal p){var r=app.ingest(a,p.getName());return ResponseEntity.status(r.created()?201:200).body(r);}
 @GetMapping("/incidents/{id}") public IncidentView get(@PathVariable UUID id){return app.get(id);}
 @PostMapping("/incidents/{id}/status") public IncidentView status(@PathVariable UUID id,@RequestBody @Valid Transition t,Principal p){return app.transition(id,t,p.getName());}
 @PostMapping("/incidents/{id}/assign") public IncidentView assign(@PathVariable UUID id,@RequestBody @Valid Assignment a,Principal p){return app.assign(id,a,p.getName());}
 @PostMapping("/incidents/{id}/notes") public IncidentView note(@PathVariable UUID id,@RequestBody @Valid Note n,Principal p){return app.note(id,n,p.getName());}
 @PostMapping("/incidents/{id}/logs") public IncidentView logs(@PathVariable UUID id,@RequestBody @NotEmpty @Size(max=30) List<@Valid LogInput> logs,Principal p){return app.appendLogs(id,logs,p.getName());}
 @PostMapping("/incidents/{id}/rca") public IncidentView rca(@PathVariable UUID id,Principal p){return app.requestRca(id,p.getName());}
 @PostMapping("/internal/incidents/{id}/rca") public IncidentView complete(@PathVariable UUID id,@RequestBody RcaCallback r){return app.completeRca(id,r);}
 @GetMapping("/summary") public Summary summary(){return app.summary();}
 @GetMapping("/services") public List<ServiceView> services(){return app.serviceViews();}
 @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) public void register(@RequestBody @Valid ServiceInput i){app.register(i);}
 @PostMapping("/services/{slug}/telemetry") @ResponseStatus(HttpStatus.NO_CONTENT) public void telemetry(@PathVariable String slug,@RequestBody @Valid Telemetry t){app.telemetry(slug,t);}
}
