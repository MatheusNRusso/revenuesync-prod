import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicProfile } from '../models/discover/public-profile.model';
import { MerchantDetail } from '../models/merchant-detail.model';

@Injectable({ providedIn: 'root' })
export class PublicProfileService {

  private readonly publicUrl = '/api/public/profiles';
  private readonly meUrl     = '/api/me/public-profile';

  constructor(private readonly http: HttpClient) {}

  // ── Public ────────────────────────────────────────────────────────────────

  listProfiles(category?: string, search?: string): Observable<PublicProfile[]> {
    let params = new HttpParams();
    if (category && category !== 'All') params = params.set('category', category);
    if (search   && search.trim())      params = params.set('search',   search.trim());
    return this.http.get<PublicProfile[]>(this.publicUrl, { params });
  }

  getProfileBySlug(slug: string): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`${this.publicUrl}/${slug}`);
  }

  getMerchantDetail(merchantId: number): Observable<MerchantDetail> {
    return this.http.get<MerchantDetail>(`/api/public/merchants/id/${merchantId}`);
  }

  // ── Authenticated ─────────────────────────────────────────────────────────

  getMyProfile(): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(this.meUrl);
  }

  upsertMyProfile(data: Partial<PublicProfile>): Observable<PublicProfile> {
    return this.http.put<PublicProfile>(this.meUrl, data);
  }

  setVisibility(isPublic: boolean): Observable<PublicProfile> {
    return this.http.patch<PublicProfile>(
      `${this.meUrl}/visibility`,
      null,
      { params: new HttpParams().set('isPublic', String(isPublic)) }
    );
  }
}
