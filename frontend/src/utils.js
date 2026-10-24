export const transitions={OPEN:['ACKNOWLEDGED'],ACKNOWLEDGED:['INVESTIGATING'],INVESTIGATING:['MITIGATED','RESOLVED'],MITIGATED:['INVESTIGATING','RESOLVED'],RESOLVED:[]};
export const label=value=>String(value??'').replaceAll('_',' ').toLowerCase().replace(/^./,c=>c.toUpperCase());
export function minutes(value){return value==null?'—':value<1?'<1 min':`${Math.round(value)} min`;}
export function age(date,now=Date.now()){const seconds=Math.max(0,(now-new Date(date).getTime())/1000);return seconds<60?'Just now':seconds<3600?`${Math.floor(seconds/60)}m ago`:seconds<86400?`${Math.floor(seconds/3600)}h ago`:`${Math.floor(seconds/86400)}d ago`;}
export function logInputs(text,now=new Date().toISOString()){return text.split('\n').filter(s=>s.trim()).slice(0,30).map(message=>({timestamp:now,level:/error|exception|fail|timeout/i.test(message)?'ERROR':'INFO',message:message.slice(0,4000),traceId:''}));}
export function incidentQuery(filters,page=0){const q=new URLSearchParams({page:String(page),size:'12'});Object.entries(filters).forEach(([k,v])=>{if(v)q.set(k,v);});return q.toString();}
