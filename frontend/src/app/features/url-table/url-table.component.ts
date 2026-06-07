import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ShortUrl } from '../../core/api/url-api.service';

@Component({
  selector: 'lf-url-table',
  imports: [DatePipe],
  template: `
    <div class="table-shell">
      <table>
        <thead>
          <tr>
            <th>Short URL</th>
            <th>Destination</th>
            <th>Status</th>
            <th>Created</th>
            <th>Expires</th>
            <th class="actions-heading">Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (url of urls; track url.id) {
            <tr [class.selected]="selectedCode === url.shortCode">
              <td>
                <button type="button" class="code-button" (click)="select.emit(url.shortCode)">
                  {{ url.shortCode }}
                </button>
              </td>
              <td class="destination">
                <span>{{ url.originalUrl }}</span>
              </td>
              <td>
                <span class="status-pill" [class.expired]="status(url) === 'Expired'" [class.never]="status(url) === 'Never expires'">
                  {{ status(url) }}
                </span>
              </td>
              <td>{{ url.createdAt | date:'MMM d, y, h:mm a' }}</td>
              <td>{{ url.expiresAt ? (url.expiresAt | date:'MMM d, y, h:mm a') : 'Never' }}</td>
              <td>
                <div class="row-actions">
                  <button type="button" title="Copy short URL" aria-label="Copy short URL" (click)="copy.emit(url)">Copy</button>
                  <button type="button" title="Open short URL" aria-label="Open short URL" (click)="open.emit(url)">Open</button>
                  <button type="button" title="View analytics" aria-label="View analytics" (click)="select.emit(url.shortCode)">View</button>
                </div>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6">
                <div class="empty-state">
                  <strong>No URLs found</strong>
                  <span>Create a short URL or adjust the current filters.</span>
                </div>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class UrlTableComponent {
  @Input({ required: true }) urls: ShortUrl[] = [];
  @Input() selectedCode: string | null = null;
  @Output() select = new EventEmitter<string>();
  @Output() copy = new EventEmitter<ShortUrl>();
  @Output() open = new EventEmitter<ShortUrl>();

  status(url: ShortUrl): 'Active' | 'Expired' | 'Never expires' {
    if (!url.expiresAt) {
      return 'Never expires';
    }
    return new Date(url.expiresAt).getTime() <= Date.now() ? 'Expired' : 'Active';
  }
}
