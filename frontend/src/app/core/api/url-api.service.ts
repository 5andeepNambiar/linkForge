import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CreateUrlRequest {
  url: string;
  expiresAt?: string | null;
  ownerId?: string | null;
}

export interface ShortUrl {
  id: string;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  expiresAt?: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class UrlApiService {
  constructor(private readonly http: HttpClient) {}

  create(request: CreateUrlRequest): Observable<ShortUrl> {
    return this.http.post<ShortUrl>(`${environment.urlApiBase}/urls`, request);
  }

  list(ownerId = 'demo-user'): Observable<ShortUrl[]> {
    return this.http.get<ShortUrl[]>(`${environment.urlApiBase}/urls`, { params: { ownerId } });
  }
}
