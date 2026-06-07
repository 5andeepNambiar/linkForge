import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DailyPoint {
  date: string;
  clicks: number;
}

export interface RecentClick {
  timestamp: string;
  ipAddress: string;
  deviceType: string;
  browser: string;
  os: string;
  country: string;
}

export interface UrlAnalytics {
  shortCode: string;
  totalClicks: number;
  lastClickedAt?: string | null;
  daily: DailyPoint[];
  recentClicks: RecentClick[];
}

@Injectable({ providedIn: 'root' })
export class AnalyticsApiService {
  constructor(private readonly http: HttpClient) {}

  get(shortCode: string, from?: string, to?: string): Observable<UrlAnalytics> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<UrlAnalytics>(`${environment.analyticsApiBase}/analytics/urls/${shortCode}`, { params });
  }
}
