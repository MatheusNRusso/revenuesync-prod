import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const STORAGE_KEY = 'rs-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  private readonly _isDark$ = new BehaviorSubject<boolean>(this.loadPreference());

  readonly isDark$ = this._isDark$.asObservable();

  get isDark(): boolean {
    return this._isDark$.value;
  }

  toggle(): void {
    this.setDark(!this._isDark$.value);
  }

  setDark(dark: boolean): void {
    this._isDark$.next(dark);
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
    document.body.classList.toggle('light-mode', !dark);
  }

  private loadPreference(): boolean {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const dark = stored === 'dark';
      document.body.classList.toggle('light-mode', !dark);
      return dark;
    }
    // default: dark
    return true;
  }
}
