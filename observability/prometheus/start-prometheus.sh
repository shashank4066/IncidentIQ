#!/bin/sh
set -eu
printf '%s' "$ADMIN_PASSWORD" > /tmp/api-password
exec /bin/prometheus --config.file=/bootstrap/prometheus.yml --storage.tsdb.path=/prometheus
