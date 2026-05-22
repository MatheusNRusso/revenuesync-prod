import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Conversion } from '../models/conversion.model';

@Injectable({
  providedIn: 'root'
})
export class ConversionService {

  private apiUrl = '/api/conversions';

  constructor(private http: HttpClient) {}

  getPayments(): Observable<Conversion[]> {
    return this.http.get<Conversion[]>(this.apiUrl);
  }


  /**
   * Fetches all conversions from the backend API.
   * @returns Observable of Conversion array
   */
  getConversions(): Observable<Conversion[]> {
    return this.http.get<Conversion[]>(this.apiUrl);
  }

  /**
   * Fetches conversions filtered by platform.
   * @param platform - The platform to filter by ('META' or 'GOOGLE')
   * @returns Observable of Conversion array
   */
  getConversionsByPlatform(platform: string): Observable<Conversion[]> {
    return this.http.get<Conversion[]>(`${this.apiUrl}/platform/${platform}`);
  }

  /**
   * Fetches conversions filtered by payment ID.
   * @param paymentId - The payment ID to filter by
   * @returns Observable of Conversion array
   */
  getConversionsByPaymentId(paymentId: number): Observable<Conversion[]> {
    return this.http.get<Conversion[]>(`${this.apiUrl}/payment/${paymentId}`);
  }

  /**
   * Exports conversions to CSV format.
   * @returns Observable of Blob containing CSV file
   */
  exportConversionsCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/csv`, {
      responseType: 'blob'
    });
  }

}
