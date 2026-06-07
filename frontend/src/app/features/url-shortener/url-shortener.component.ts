import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ShortUrl, UrlApiService } from '../../core/api/url-api.service';

@Component({
  selector: 'lf-url-shortener',
  imports: [ReactiveFormsModule],
  template: `
    <form class="shortener" [formGroup]="form" (ngSubmit)="submit()">
      <div class="field">
        <label for="long-url">Destination URL</label>
        <input
          id="long-url"
          formControlName="url"
          type="url"
          autocomplete="url"
          placeholder="https://example.com/product/42"
          aria-describedby="url-error"
        >
        @if (form.controls.url.touched && form.controls.url.invalid) {
          <p id="url-error" class="form-error">Enter a valid URL that starts with http:// or https://.</p>
        }
      </div>

      <div class="field-row">
        <div class="field">
          <label for="expires-at">Expiration</label>
          <input id="expires-at" formControlName="expiresAt" type="datetime-local" [min]="minExpiresAt">
        </div>
        <button class="primary-action" type="submit" [disabled]="form.invalid || loading">
          {{ loading ? 'Creating...' : 'Create short URL' }}
        </button>
      </div>

      @if (errorMessage) {
        <p class="form-error">{{ errorMessage }}</p>
      }

      @if (createdUrl) {
        <div class="success-strip" aria-live="polite">
          <div>
            <span>Created</span>
            <strong>{{ createdUrl.shortUrl }}</strong>
          </div>
          <div class="inline-actions">
            <button type="button" class="ghost-action" (click)="copy(createdUrl.shortUrl)">
              {{ copied ? 'Copied' : 'Copy' }}
            </button>
            <button type="button" class="ghost-action" (click)="open(createdUrl.shortUrl)">Open</button>
          </div>
        </div>
      }
    </form>
  `
})
export class UrlShortenerComponent {
  private readonly fb = inject(FormBuilder);
  private readonly urls = inject(UrlApiService);

  @Output() created = new EventEmitter<ShortUrl>();

  loading = false;
  copied = false;
  errorMessage = '';
  createdUrl: ShortUrl | null = null;
  readonly minExpiresAt = this.toDateTimeLocal(new Date(Date.now() + 60_000));
  form = this.fb.nonNullable.group({
    url: ['', [Validators.required, Validators.pattern(/^https?:\/\/[\w.-]+(:\d+)?(\/.*)?$/i)]],
    expiresAt: ['']
  });

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    this.copied = false;
    this.errorMessage = '';

    const value = this.form.getRawValue();
    const expiresAt = value.expiresAt ? new Date(value.expiresAt) : null;
    if (expiresAt && expiresAt.getTime() <= Date.now()) {
      this.loading = false;
      this.errorMessage = 'Choose an expiration time in the future, or leave it blank.';
      return;
    }

    this.urls.create({
      url: value.url.trim(),
      expiresAt: expiresAt ? expiresAt.toISOString() : null,
      ownerId: 'demo-user'
    }).subscribe({
      next: (url) => {
        this.createdUrl = url;
        this.created.emit(url);
        this.form.reset({ url: '', expiresAt: '' });
      },
      complete: () => (this.loading = false),
      error: () => {
        this.errorMessage = 'Could not create the short URL. Check the URL and expiration time.';
        this.loading = false;
      }
    });
  }

  copy(value: string): void {
    navigator.clipboard?.writeText(value).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 1800);
    });
  }

  open(value: string): void {
    window.open(value, '_blank', 'noopener,noreferrer');
  }

  private toDateTimeLocal(date: Date): string {
    const offsetMs = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
  }
}
