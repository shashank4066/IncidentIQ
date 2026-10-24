import {test,expect} from '@playwright/test';
test('operator can declare, investigate, analyze and resolve an incident',async({page})=>{
 await page.goto('/');await page.getByLabel('Username',{exact:true}).fill('admin');await page.getByLabel('Password',{exact:true}).fill(process.env.ADMIN_PASSWORD||'demo-admin');await page.getByRole('button',{name:'Sign in',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Incident command',exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Declare incident',exact:true}).click();
 let dialog=page.getByRole('dialog');const title='E2E payment timeout '+Date.now();
 await dialog.getByLabel('Incident title').fill(title);await dialog.getByLabel('Affected service').selectOption('payments');await dialog.getByLabel('Description').fill('Automated end-to-end verification');await dialog.getByLabel('Initial log evidence').fill('ERROR connection pool timeout trace=e2e');await dialog.getByRole('button',{name:'Declare incident',exact:true}).click();
 dialog=page.getByRole('dialog');await expect(dialog.getByRole('heading',{name:title})).toBeVisible();
 for(const next of ['Acknowledged','Investigating']){await dialog.getByRole('button',{name:next,exact:true}).click();await dialog.getByLabel('Response note').fill('E2E verified '+next);await dialog.getByRole('button',{name:'Save response'}).click();await expect(dialog.locator('.drawer-badges')).toContainText(next);}
 await dialog.getByRole('tab',{name:'AI investigation'}).click();await dialog.getByRole('button',{name:'Generate analysis'}).click();await expect(dialog.getByRole('heading',{name:'Possible root causes'})).toBeVisible({timeout:60000});await expect(dialog.locator('.analysis')).toContainText('connection pool');
 await dialog.getByRole('button',{name:'Resolved',exact:true}).click();await dialog.getByLabel('Response note').fill('Recovery verified by end-to-end test.');await dialog.getByRole('button',{name:'Save response'}).click();await expect(dialog.locator('.drawer-badges')).toContainText('Resolved');
});
test('viewer has read-only controls',async({page})=>{
 await page.goto('/');await page.getByLabel('Username',{exact:true}).fill('viewer');await page.getByLabel('Password',{exact:true}).fill(process.env.VIEWER_PASSWORD||'demo-viewer');await page.getByRole('button',{name:'Sign in',exact:true}).click();await expect(page.getByRole('heading',{name:'Incident command',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Declare incident',exact:true})).toHaveCount(0);
});
