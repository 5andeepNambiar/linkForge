import { DatePipe, DecimalPipe } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Chart } from 'chart.js/auto';
import { AnalyticsApiService, RecentClick, UrlAnalytics } from '../../core/api/analytics-api.service';
import { ShortUrl, UrlApiService } from '../../core/api/url-api.service';
import { UrlShortenerComponent } from '../url-shortener/url-shortener.component';
import { UrlTableComponent } from '../url-table/url-table.component';

type SortMode = 'newest' | 'expiry' | 'code';

interface Breakdown {
  label: string;
  count: number;
}

@Component({
  selector: 'lf-analytics-dashboard',
  imports: [DatePipe, DecimalPipe, FormsModule, UrlShortenerComponent, UrlTableComponent],
  template: `
    <div class="app-shell">
      <aside class="sidebar" aria-label="Primary navigation">
        <div class="brand-block">
          <div class="brand-mark">LF</div>
          <div>
            <strong>LinkForge</strong>
            <span>URL Operations</span>
          </div>
        </div>

        <nav>
          <a class="active" href="#overview">Overview</a>
          <a href="#links">Links</a>
          <a href="#analytics">Analytics</a>
          <a href="#events">Clickstream</a>
        </nav>

        <div class="sidebar-status">
          <span class="health-dot"></span>
          <div>
            <strong>System online</strong>
            <span>Redis · Kafka · Postgres</span>
          </div>
        </div>
      </aside>

      <main>
        <section id="overview" class="summary-grid">
          <div class="metric-card">
            <span>Total links</span>
            <strong>{{ urls.length | number }}</strong>
          </div>
          <div class="metric-card">
            <span>Active links</span>
            <strong>{{ activeCount | number }}</strong>
          </div>
          <div class="metric-card">
            <span>Selected clicks</span>
            <strong>{{ (analytics?.totalClicks ?? 0) | number }}</strong>
          </div>
          <div class="metric-card">
            <span>Last click</span>
            <strong>{{ analytics?.lastClickedAt ? (analytics?.lastClickedAt | date:'MMM d, h:mm a') : 'None' }}</strong>
          </div>
        </section>

        <section id="links" class="layout-grid">
          <div class="panel create-panel">
            <div class="panel-heading">
              <div>
                <p>Create</p>
                <h2>Short URL</h2>
              </div>
              <span class="panel-kicker">Write-through cache</span>
            </div>
            <lf-url-shortener (created)="onCreated($event)" />
          </div>

          <div class="panel links-panel">
            <div class="panel-heading">
              <div>
                <p>Manage</p>
                <h2>Generated URLs</h2>
              </div>
              <div class="toolbar">
                <label>
                  <span>Sort</span>
                  <select [(ngModel)]="sortMode" aria-label="Sort URLs">
                    <option value="newest">Newest</option>
                    <option value="expiry">Expiry</option>
                    <option value="code">Short code</option>
                  </select>
                </label>
              </div>
            </div>

            @if (urlError) {
              <div class="error-banner">{{ urlError }}</div>
            }

            <lf-url-table
              [urls]="filteredUrls"
              [selectedCode]="selectedCode"
              (select)="selectUrl($event)"
              (copy)="copyShortUrl($event)"
              (open)="openShortUrl($event)"
            />
          </div>
        </section>

        <section id="analytics" class="panel analytics-panel">
          <div class="panel-heading">
            <div>
              <p>Analytics</p>
              <h2>{{ selectedCode ? selectedCode : 'No URL selected' }}</h2>
            </div>
            <div class="filters">
              <input [(ngModel)]="from" type="date" aria-label="From date">
              <input [(ngModel)]="to" type="date" aria-label="To date">
              <button type="button" class="secondary-action" [disabled]="!selectedCode || loadingAnalytics" (click)="loadAnalytics()">
                Apply
              </button>
            </div>
          </div>

          @if (analyticsError) {
            <div class="error-banner">{{ analyticsError }}</div>
          }

          <div class="analytics-grid">
            <div class="chart-panel">
              @if (selectedCode) {
                <canvas #trendCanvas></canvas>
                @if (!loadingAnalytics && (analytics?.daily?.length ?? 0) === 0) {
                  <div class="chart-empty">No trend data yet</div>
                }
              } @else {
                <div class="empty-state tall">
                  <strong>Select a URL</strong>
                  <span>Analytics will appear here after redirect traffic is recorded.</span>
                </div>
              }
            </div>

            <div class="breakdown-panel">
              <div class="breakdown-card">
                <span>Devices</span>
                @for (item of deviceBreakdown; track item.label) {
                  <div class="breakdown-row">
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.count | number }}</span>
                  </div>
                } @empty {
                  <div class="muted-row">No device data</div>
                }
              </div>
              <div class="breakdown-card">
                <span>Browsers</span>
                @for (item of browserBreakdown; track item.label) {
                  <div class="breakdown-row">
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.count | number }}</span>
                  </div>
                } @empty {
                  <div class="muted-row">No browser data</div>
                }
              </div>
              <div class="breakdown-card">
                <span>Operating systems</span>
                @for (item of osBreakdown; track item.label) {
                  <div class="breakdown-row">
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.count | number }}</span>
                  </div>
                } @empty {
                  <div class="muted-row">No OS data</div>
                }
              </div>
            </div>
          </div>
        </section>

        <section id="events" class="panel">
          <div class="panel-heading">
            <div>
              <p>Clickstream</p>
              <h2>Recent events</h2>
            </div>
            @if (copiedMessage) {
              <span class="copy-toast">{{ copiedMessage }}</span>
            }
          </div>

          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>IP</th>
                  <th>Device</th>
                  <th>Browser</th>
                  <th>OS</th>
                  <th>Geo</th>
                </tr>
              </thead>
              <tbody>
                @for (click of analytics?.recentClicks ?? []; track click.timestamp + click.ipAddress) {
                  <tr>
                    <td>{{ click.timestamp | date:'MMM d, y, h:mm:ss a' }}</td>
                    <td>{{ click.ipAddress || 'Unknown' }}</td>
                    <td>{{ click.deviceType || 'Unknown' }}</td>
                    <td>{{ click.browser || 'Unknown' }}</td>
                    <td>{{ click.os || 'Unknown' }}</td>
                    <td>{{ click.country || 'Unknown' }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="6">
                      <div class="empty-state">
                        <strong>No click events</strong>
                        <span>Open a short URL to generate the first event.</span>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  `
})
export class AnalyticsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('trendCanvas') trendCanvas?: ElementRef<HTMLCanvasElement>;

  urls: ShortUrl[] = [];
  selectedCode: string | null = null;
  analytics: UrlAnalytics | null = null;
  searchTerm = '';
  sortMode: SortMode = 'newest';
  from = '';
  to = '';
  loadingUrls = false;
  loadingAnalytics = false;
  urlError = '';
  analyticsError = '';
  copiedMessage = '';
  private chart?: Chart;

  constructor(private readonly urlApi: UrlApiService, private readonly analyticsApi: AnalyticsApiService) {}

  get filteredUrls(): ShortUrl[] {
    const needle = this.searchTerm.trim().toLowerCase();
    const filtered = this.urls.filter((url) => {
      if (!needle) return true;
      return url.shortCode.toLowerCase().includes(needle) || url.originalUrl.toLowerCase().includes(needle);
    });

    return filtered.sort((left, right) => {
      if (this.sortMode === 'code') {
        return left.shortCode.localeCompare(right.shortCode);
      }
      if (this.sortMode === 'expiry') {
        return this.expiryTime(left) - this.expiryTime(right);
      }
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
  }

  get activeCount(): number {
    return this.urls.filter((url) => !url.expiresAt || new Date(url.expiresAt).getTime() > Date.now()).length;
  }

  get deviceBreakdown(): Breakdown[] {
    return this.breakdown('deviceType');
  }

  get browserBreakdown(): Breakdown[] {
    return this.breakdown('browser');
  }

  get osBreakdown(): Breakdown[] {
    return this.breakdown('os');
  }

  ngOnInit(): void {
    this.loadUrls();
  }

  ngAfterViewInit(): void {
    this.renderChart();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  onCreated(url: ShortUrl): void {
    this.urls = [url, ...this.urls.filter((item) => item.id !== url.id)];
    this.copiedMessage = 'Short URL created';
    this.selectUrl(url.shortCode);
    this.clearToast();
  }

  refresh(): void {
    this.loadUrls(true);
    if (this.selectedCode) {
      this.loadAnalytics();
    }
  }

  loadUrls(keepSelection = false): void {
    this.loadingUrls = true;
    this.urlError = '';
    this.urlApi.list().subscribe({
      next: (urls) => {
        this.urls = urls;
        const selectedStillExists = this.selectedCode && urls.some((url) => url.shortCode === this.selectedCode);
        if (!keepSelection || !selectedStillExists) {
          this.selectedCode = selectedStillExists ? this.selectedCode : urls[0]?.shortCode ?? null;
        }
        if (this.selectedCode) {
          this.loadAnalytics();
        }
      },
      error: () => {
        this.urlError = 'URL service is unavailable. Check the container logs and try again.';
      },
      complete: () => {
        this.loadingUrls = false;
      }
    });
  }

  selectUrl(shortCode: string): void {
    this.selectedCode = shortCode;
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    if (!this.selectedCode) return;
    this.loadingAnalytics = true;
    this.analyticsError = '';
    this.analyticsApi.get(this.selectedCode, this.from, this.to).subscribe({
      next: (analytics) => {
        this.analytics = analytics;
        this.renderChart();
      },
      error: () => {
        this.analyticsError = 'Analytics service is unavailable. Redirects still work while analytics recovers.';
        this.analytics = null;
        this.renderChart();
      },
      complete: () => {
        this.loadingAnalytics = false;
      }
    });
  }

  copyShortUrl(url: ShortUrl): void {
    navigator.clipboard?.writeText(url.shortUrl).then(() => {
      this.copiedMessage = `${url.shortCode} copied`;
      this.clearToast();
    });
  }

  openShortUrl(url: ShortUrl): void {
    window.open(url.shortUrl, '_blank', 'noopener,noreferrer');
  }

  private expiryTime(url: ShortUrl): number {
    return url.expiresAt ? new Date(url.expiresAt).getTime() : Number.MAX_SAFE_INTEGER;
  }

  private breakdown(key: keyof Pick<RecentClick, 'deviceType' | 'browser' | 'os'>): Breakdown[] {
    const counts = new Map<string, number>();
    for (const click of this.analytics?.recentClicks ?? []) {
      const label = click[key] || 'Unknown';
      counts.set(label, (counts.get(label) ?? 0) + 1);
    }
    return [...counts.entries()]
      .map(([label, count]) => ({ label, count }))
      .sort((left, right) => right.count - left.count)
      .slice(0, 4);
  }

  private clearToast(): void {
    setTimeout(() => (this.copiedMessage = ''), 2200);
  }

  private renderChart(): void {
    const canvas = this.trendCanvas?.nativeElement;
    if (!canvas) return;
    const labels = this.analytics?.daily.map((point) => point.date) ?? [];
    const values = this.analytics?.daily.map((point) => point.clicks) ?? [];
    this.chart?.destroy();
    this.chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Clicks',
          data: values,
          borderColor: '#1d4ed8',
          backgroundColor: 'rgba(29, 78, 216, 0.1)',
          pointRadius: 3,
          pointHoverRadius: 5,
          borderWidth: 2,
          tension: 0.28,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { intersect: false, mode: 'index' }
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#64748b' } },
          y: { beginAtZero: true, ticks: { precision: 0, color: '#64748b' }, grid: { color: '#eef2f7' } }
        }
      }
    });
  }
}
