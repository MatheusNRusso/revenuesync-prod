import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminConversion,
  AdminDashboard,
  AdminLead,
  AdminMerchant,
  AdminPayment,
  AdminUser
} from '../models/admin/admin-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = '/api/admin';

  constructor(private readonly http: HttpClient) { }

  getDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.apiUrl}/dashboard`);
  }

  getPayments(): Observable<AdminPayment[]> {
    return this.http.get<AdminPayment[]>(`${this.apiUrl}/payments`);
  }

  getLeads(): Observable<AdminLead[]> {
    return this.http.get<AdminLead[]>(`${this.apiUrl}/leads`);
  }

  getMerchants(): Observable<AdminMerchant[]> {
    return this.http.get<AdminMerchant[]>(`${this.apiUrl}/merchants`);
  }

  getConversions(): Observable<AdminConversion[]> {
    return this.http.get<AdminConversion[]>(`${this.apiUrl}/conversions`);
  }

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/users`);
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${userId}`);
  }

  deleteMerchant(merchantId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/merchants/${merchantId}`);
  }
}
