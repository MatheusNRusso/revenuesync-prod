import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicMerchant } from '../models/discover/public-merchant.model';

@Injectable({
  providedIn: 'root'
})
export class DiscoverService {
  private readonly apiUrl = '/api/public';

  constructor(private readonly http: HttpClient) {}

  getPublicMerchants(): Observable<PublicMerchant[]> {
    return this.http.get<PublicMerchant[]>(`${this.apiUrl}/merchants`);
  }
}
