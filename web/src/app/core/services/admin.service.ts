import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminConversion,
  AdminDashboard,
  AdminLead,
  AdminMerchant,
  AdminPayment
} from '../models/admin/admin-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = '/api/admin';

  constructor(private readonly http: HttpClient) {}

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
}
