import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.html',
  styleUrls: ['./landing.scss']
})
export class LandingComponent implements OnInit, OnDestroy {

  paymentCount = 2841;
  solVolume    = 1847.32;
  flicker      = false;
  activeStep   = 0;

  private terminalInterval: any;
  private stepInterval: any;

  stats = [
    { label: 'sol volume',   value: '1.8',  unit: 'K SOL', delta: '+24% YoY',    dir: 'up' },
    { label: 'active teams', value: '2,481', unit: '',      delta: '+312 this mo', dir: 'up' },
    { label: 'events / sec', value: '47',    unit: 'k',     delta: 'real-time',    dir: 'up' },
    { label: 'avg setup',    value: '4.2',   unit: 'min',   delta: '-38% vs v1',   dir: 'down' },
  ];

  features = [
    { tag: '[01] payments',    title: 'Solana Pay streams',  chips: ['Solana Pay', 'Multi-wallet', 'Webhooks'], desc: 'Every on-chain payment, refund, and settlement — normalized across wallets and pushed to a single live ledger in real-time.',
      icon: '<rect x="2" y="6" width="20" height="13" rx="1"/><path d="M2 10h20M6 15h4"/>' },
    { tag: '[02] conversions', title: 'Funnel attribution',  chips: ['UTM', 'Cohorts', 'Multi-touch'],          desc: 'Map sessions to on-chain transactions. See which campaigns produced revenue, not just signups, with cohort breakdowns.',
      icon: '<path d="M3 4h18l-7 8v7l-4-2v-5L3 4z"/>' },
    { tag: '[03] leads',       title: 'Lead pipeline',       chips: ['Scoring', 'CRM sync', 'Webhooks'],        desc: 'Score, segment, and route leads automatically. Sync to HubSpot, Salesforce, or your warehouse on every event.',
      icon: '<circle cx="9" cy="8" r="3.5"/><path d="M3 20c0-3 2.7-5 6-5s6 2 6 5M16 11a3 3 0 1 0 0-6M21 20c0-2.5-1.8-4.4-4-4.9"/>' },
    { tag: '[04] analytics',   title: 'Revenue analytics',   chips: ['MRR / ARR', 'Cohorts', 'NRR'],           desc: 'MRR, ARR, NRR, churn, LTV, payback — pre-built and accurate. Slice by plan, geography, or any custom dimension.',
      icon: '<path d="M3 3v18h18"/><path d="M7 14l4-5 3 3 5-7"/>' },
    { tag: '[05] csv',         title: 'Export anywhere',     chips: ['CSV', 'Parquet', 'Snowflake'],            desc: 'CSV, Parquet, or direct push to Snowflake, BigQuery, and Postgres. Schedule, encrypt, and audit every export.',
      icon: '<path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6M8 13h8M8 17h5"/>' },
    { tag: '[06] security',    title: 'Secure by default',   chips: ['SOC 2', 'GDPR', 'SSO'],                  desc: 'SOC 2 Type II, GDPR, and HIPAA-ready. Read-only wallet scopes, tokenized PII, and full SSO + audit logs.',
      icon: '<path d="M12 2l8 4v6c0 5-3.5 9-8 10-4.5-1-8-5-8-10V6l8-4z"/><path d="M9 12l2 2 4-4"/>' },
  ];

  steps = [
    { n: '01', title: 'Connect',   desc: 'Generate a Solana Pay QR code. 30 seconds, zero config.',    icon: '<path d="M9 2v6M15 2v6M6 8h12v4a6 6 0 0 1-12 0V8zM12 18v4"/>' },
    { n: '02', title: 'Backfill',  desc: 'We replay your full on-chain history into the ledger.',       icon: '<path d="M3 12a9 9 0 0 1 15-6.7L21 8M21 3v5h-5M21 12a9 9 0 0 1-15 6.7L3 16M3 21v-5h5"/>' },
    { n: '03', title: 'Configure', desc: 'Set funnels, cohorts, and KPI rules with simple yaml.',       icon: '<path d="M4 6h10M18 6h2M4 12h2M10 12h10M4 18h14M14 4v4M8 10v4M16 16v4"/>' },
    { n: '04', title: 'Observe',   desc: 'Live dashboards, slack alerts, daily revenue digest.',        icon: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>' },
    { n: '05', title: 'Ship',      desc: 'Export to your warehouse or pipe back into your app.',        icon: '<path d="M5 13l-2 7 7-2M14 4l6 6M9 15l8-8a4 4 0 0 1 5 0l-1 1a4 4 0 0 1-1 5l-8 8-3-2-3-3z"/>' },
  ];

  bars = [18,32,24,41,35,52,47,38,56,62,48,71,65,78,82,86,94];

  constructor(
    private router: Router,
    private cdr: ChangeDetectorRef,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.terminalInterval = setInterval(() => {
      this.flicker      = !this.flicker;
      this.paymentCount += Math.floor(Math.random() * 3) + 1;
      this.solVolume    += parseFloat((Math.random() * 2.4 + 0.1).toFixed(2));
      this.cdr.detectChanges();
    }, 1800);

    this.stepInterval = setInterval(() => {
      this.activeStep = (this.activeStep + 1) % this.steps.length;
      this.cdr.detectChanges();
    }, 2400);
  }

  ngOnDestroy(): void {
    clearInterval(this.terminalInterval);
    clearInterval(this.stepInterval);
  }

  svg(content: string): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(
      `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">${content}</svg>`
    );
  }

  scrollToSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (!element) {
      return;
  }
  const headerOffset = 72;
  const elementPosition = element.getBoundingClientRect().top + window.scrollY;
  const offsetPosition = elementPosition - headerOffset;
  window.scrollTo({
    top: offsetPosition,
    behavior: 'smooth',
    });
  }

  get solFormatted(): string    { return this.solVolume.toFixed(2) + ' SOL'; }
  get paymentFormatted(): string { return this.paymentCount.toLocaleString('en-US'); }

  barBg(i: number): string      { return i > 12 ? 'var(--accent)' : 'var(--border-bright)'; }
  barOpacity(i: number): number { return i > 12 ? 1 : 0.6; }
  barHeight(h: number, i: number): string {
    if (i === 15) return (this.flicker ? 88 : 86) + '%';
    return h + '%';
  }

  goToLogin():    void { this.router.navigate(['/login']); }
  goToRegister(): void { this.router.navigate(['/register']); }
  goToLanding():  void { this.router.navigate(['/']); }
}
