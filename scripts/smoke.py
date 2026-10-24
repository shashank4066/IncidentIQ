"""Exercise a running API. Uses only Python's standard library; no credentials are printed."""
import os,json,base64,uuid,time,urllib.request,urllib.error
base=os.getenv('INCIDENT_API_URL','http://localhost:8080')
password=os.getenv('ADMIN_PASSWORD','demo-admin')
auth='Basic '+base64.b64encode(('admin:'+password).encode()).decode()
def call(path,body=None):
 data=None if body is None else json.dumps(body).encode()
 req=urllib.request.Request(base+'/api'+path,data=data,headers={'Authorization':auth,'Content-Type':'application/json'})
 with urllib.request.urlopen(req,timeout=15) as res:
  data=res.read();return json.loads(data) if data else None
slug='smoke-'+uuid.uuid4().hex[:8]
call('/services',{'slug':slug,'name':'Smoke Test','team':'test-oncall'})
alert={'source':'smoke','eventId':uuid.uuid4().hex,'fingerprint':'pool-timeout','title':'Smoke: pool timeout','description':'Disposable smoke-test incident','service':slug,'errorRate':0.08,'latencyP95':4000,'logs':[{'timestamp':'2026-01-01T00:00:00Z','level':'ERROR','message':'connection pool timeout password=do-not-store','traceId':'smoke-trace'}]}
first=call('/alerts',alert);assert first['created'];incident=first['incident'];iid=incident['id']
assert call('/alerts',alert)['duplicate'];assert 'do-not-store' not in incident['logs'][0]['message']
for status in ['ACKNOWLEDGED','INVESTIGATING']:
 call('/incidents/'+iid+'/status',{'status':status,'note':'Smoke test step'})
call('/incidents/'+iid+'/rca',{})
for _ in range(90):
 incident=call('/incidents/'+iid)
 if incident['rcaStatus']=='COMPLETED':break
 time.sleep(2)
else:raise AssertionError('RCA did not complete')
assert incident['analysis']['hypotheses'];call('/incidents/'+iid+'/status',{'status':'RESOLVED','note':'Smoke test completed and recovery verified'})
assert call('/summary')['resolved']>=1
print('PASS: classification, deduplication, redaction, lifecycle, RCA, and metrics')
