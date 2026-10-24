import {test} from 'node:test';import assert from 'node:assert/strict';import {transitions,minutes,age,logInputs,incidentQuery} from '../src/utils.js';
test('resolved incidents have no outgoing transitions',()=>assert.deepEqual(transitions.RESOLVED,[]));
test('unknown MTTR is not represented as zero',()=>{assert.equal(minutes(null),'—');assert.equal(minutes(0.3),'<1 min');});
test('future clock skew does not show negative time',()=>assert.equal(age('2030-01-01',0),'Just now'));
test('log input respects API limits',()=>{const logs=logInputs(Array(40).fill('ERROR timeout').join('\n'),'2026-01-01T00:00:00Z');assert.equal(logs.length,30);assert.equal(logs[0].level,'ERROR');});
test('filters preserve encoded search characters',()=>{const q=new URLSearchParams(incidentQuery({search:'a&b',severity:'SEV1',status:''},2));assert.equal(q.get('search'),'a&b');assert.equal(q.get('page'),'2');assert.equal(q.has('status'),false);});
