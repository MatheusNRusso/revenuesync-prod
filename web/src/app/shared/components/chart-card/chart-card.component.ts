import {
  Component, Input, AfterViewInit, OnDestroy, OnChanges, SimpleChanges
} from '@angular/core';
import Chart, { ChartConfiguration, ChartType } from 'chart.js/auto';

@Component({
  selector: 'app-chart-card',
  standalone: true,
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <h3>{{ title }}</h3>
        <span class="chart-badge">{{ labels.length }} data pts</span>
      </div>
      <div class="chart-body">
        <canvas [id]="chartId" width="400" height="180"></canvas>
      </div>
    </div>
  `,
  styleUrls: ['./chart-card.component.scss']
})
export class ChartCardComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input() title!: string;
  @Input() chartId!: string;
  @Input() labels: string[] = [];
  @Input() data: number[] = [];
  @Input() chartType: ChartType = 'line';
  @Input() isDark = true;

  private chart: Chart | null = null;

  ngAfterViewInit(): void {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.chart) return;

    // Tema mudou → recria o chart com novas cores
    if (changes['isDark'] && !changes['isDark'].firstChange) {
      this.chart.destroy();
      this.chart = null;
      // Pequeno delay para garantir que o DOM refletiu o novo tema
      setTimeout(() => this.createChart(), 50);
      return;
    }

    if (changes['labels'] || changes['data']) {
      this.updateChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  private get colors() {
    if (this.isDark) {
      return {
        accent:       '#00d4aa',
        accentHover:  '#00f0c0',
        gridColor:    'rgba(255,255,255,0.04)',
        tickColor:    '#4a5568',
        tooltipBg:    '#121828',
        tooltipBorder:'rgba(0,212,170,0.3)',
        tooltipTitle: '#8899b0',
        tooltipBody:  '#00d4aa',
        pointBorder:  '#080c14',
        barBg:        'rgba(0,212,170,0.15)',
        gradientTop:  'rgba(0,212,170,0.22)',
        gradientBot:  'rgba(0,212,170,0.0)',
      };
    }
    return {
      accent:       '#00a884',
      accentHover:  '#00c49a',
      gridColor:    'rgba(15,23,42,0.06)',
      tickColor:    '#94a3b8',
      tooltipBg:    '#ffffff',
      tooltipBorder:'rgba(0,168,132,0.35)',
      tooltipTitle: '#64748b',
      tooltipBody:  '#00a884',
      pointBorder:  '#eef2f8',
      barBg:        'rgba(0,168,132,0.12)',
      gradientTop:  'rgba(0,168,132,0.18)',
      gradientBot:  'rgba(0,168,132,0.0)',
    };
  }

  private createChart(): void {
    const canvas = document.getElementById(this.chartId) as HTMLCanvasElement;
    if (!canvas) return;

    const isBar = this.chartType === 'bar';
    const c = this.colors;

    const config: ChartConfiguration = {
      type: this.chartType,
      data: {
        labels: this.labels,
        datasets: [{
          label: this.title,
          data: this.data,
          borderColor: c.accent,
          backgroundColor: isBar
            ? c.barBg
            : (ctx: any) => {
              const gradient = ctx.chart.ctx.createLinearGradient(0, 0, 0, 180);
              gradient.addColorStop(0, c.gradientTop);
              gradient.addColorStop(1, c.gradientBot);
              return gradient;
            },
          borderWidth: isBar ? 0 : 1.5,
          tension: 0.45,
          fill: true,
          pointRadius: 3,
          pointBackgroundColor: c.accent,
          pointBorderColor: c.pointBorder,
          pointBorderWidth: 1.5,
          pointHoverRadius: 5,
          pointHoverBackgroundColor: c.accentHover,
          // FIX 2: limita largura das barras independente de data points
          ...(isBar ? {
            borderRadius: 4,
            borderSkipped: false,
            maxBarThickness: 36,
            barPercentage: 0.5,
            categoryPercentage: 0.6,
          } : {}),
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 400, easing: 'easeInOutQuart' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: c.tooltipBg,
            borderColor: c.tooltipBorder,
            borderWidth: 1,
            titleColor: c.tooltipTitle,
            bodyColor: c.tooltipBody,
            titleFont: { family: 'DM Mono, monospace', size: 10 },
            bodyFont: { family: 'DM Mono, monospace', size: 12, weight: 'bold' },
            padding: 10,
            cornerRadius: 6,
            callbacks: {
              label: (ctx) => {
                const y = ctx.parsed.y ?? 0;
                return `  ${y.toLocaleString('pt-BR')}`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: c.gridColor },
            border: { display: false },
            ticks: {
              color: c.tickColor,
              font: { family: 'DM Mono, monospace', size: 9 },
              maxTicksLimit: 5,
              padding: 8,
            }
          },
          x: {
            grid: { display: false },
            border: { display: false },
            ticks: {
              color: c.tickColor,
              font: { family: 'DM Mono, monospace', size: 9 },
              maxTicksLimit: 6,
              padding: 8,
            }
          }
        }
      }
    };

    this.chart = new Chart(canvas, config);
  }

  private updateChart(): void {
    if (!this.chart) return;
    this.chart.data.labels = this.labels;
    this.chart.data.datasets[0].data = this.data;
    this.chart.update('active');
  }
}
