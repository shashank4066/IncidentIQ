CREATE TABLE service_catalog (
 slug varchar(64) PRIMARY KEY, name varchar(100) NOT NULL, team varchar(100) NOT NULL,
 heartbeat_at timestamp with time zone, up boolean NOT NULL DEFAULT true,
 error_rate double precision NOT NULL DEFAULT 0, latency_p95 bigint NOT NULL DEFAULT 0, cpu_percent double precision NOT NULL DEFAULT 0
);
CREATE TABLE incidents (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, title varchar(200) NOT NULL, description varchar(6000) NOT NULL,
 service varchar(64) NOT NULL REFERENCES service_catalog(slug), fingerprint varchar(64) NOT NULL,
 severity varchar(10) NOT NULL, status varchar(20) NOT NULL, assignee varchar(100) NOT NULL,
 created_at timestamp with time zone NOT NULL, updated_at timestamp with time zone NOT NULL,
 acknowledged_at timestamp with time zone, resolved_at timestamp with time zone, occurrences integer NOT NULL,
 revision bigint NOT NULL, rca_status varchar(20) NOT NULL, rca_request_id uuid, rca_requested_at timestamp with time zone, analysis_json varchar(30000)
);
CREATE INDEX idx_incident_service_status ON incidents(service,status);
CREATE INDEX idx_incident_created ON incidents(created_at);
CREATE INDEX idx_incident_fingerprint ON incidents(service,fingerprint,status);
CREATE TABLE incident_logs (id uuid PRIMARY KEY,incident_id uuid NOT NULL REFERENCES incidents(id),timestamp timestamp with time zone NOT NULL,level varchar(10) NOT NULL,message varchar(4000) NOT NULL,trace_id varchar(128));
CREATE INDEX idx_logs_incident ON incident_logs(incident_id);
CREATE TABLE incident_timeline (id uuid PRIMARY KEY,incident_id uuid NOT NULL REFERENCES incidents(id),timestamp timestamp with time zone NOT NULL,type varchar(50) NOT NULL,actor varchar(100) NOT NULL,message varchar(2000) NOT NULL);
CREATE INDEX idx_timeline_incident ON incident_timeline(incident_id);
CREATE TABLE alert_receipts (id varchar(64) PRIMARY KEY,payload_hash varchar(64) NOT NULL,incident_id uuid NOT NULL REFERENCES incidents(id),received_at timestamp with time zone NOT NULL);
CREATE TABLE outbox_events (id uuid PRIMARY KEY,incident_id uuid NOT NULL REFERENCES incidents(id),payload text NOT NULL,created_at timestamp with time zone NOT NULL,published_at timestamp with time zone,attempts integer NOT NULL DEFAULT 0);
CREATE INDEX idx_outbox_pending ON outbox_events(published_at,created_at);
