import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./e2e',timeout:90000,expect:{timeout:15000},fullyParallel:false,retries:process.env.CI?1:0,use:{baseURL:process.env.WEB_URL||'http://127.0.0.1:3000',trace:'retain-on-failure',screenshot:'only-on-failure'},reporter:[['list'],['html',{open:'never'}]]});
