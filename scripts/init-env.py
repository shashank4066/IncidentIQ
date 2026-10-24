"""Create local Compose credentials without printing or committing them."""
from pathlib import Path
import secrets
root=Path(__file__).resolve().parents[1]
target=root/'.env'
if target.exists():
 print('.env already exists; preserved existing values.')
 raise SystemExit(0)
keys=['DATABASE_PASSWORD','REDIS_PASSWORD','ADMIN_PASSWORD','VIEWER_PASSWORD','WORKER_PASSWORD','GRAFANA_PASSWORD']
content=''.join(f'{k}={secrets.token_urlsafe(24)}\n' for k in keys)
content+='SEED_DEMO=true\nAI_BASE_URL=https://api.openai.com/v1\nAI_MODEL=gpt-4o-mini\nAI_API_KEY=\n'
target.write_text(content,encoding='utf-8')
try:target.chmod(0o600)
except OSError:pass
print('Created .env. Open it locally to obtain the admin password.')
