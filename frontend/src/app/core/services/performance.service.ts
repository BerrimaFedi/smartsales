import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalysePerformance, Performance, PerformanceCalculee, PerformanceRequest } from '../models/performance.model';

@Injectable({ providedIn: 'root' })
export class PerformanceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/performances`;

  getAll(): Observable<Performance[]> { return this.http.get<Performance[]>(this.base); }
  create(req: PerformanceRequest): Observable<Performance> { return this.http.post<Performance>(this.base, req); }
  update(id: number, req: PerformanceRequest): Observable<Performance> { return this.http.put<Performance>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }

  getCalculees(periode?: string): Observable<PerformanceCalculee[]> {
    const params = periode ? new HttpParams().set('periode', periode) : undefined;
    return this.http.get<PerformanceCalculee[]>(`${this.base}/calculees`, { params });
  }

  getCalculeeMe(periode?: string): Observable<PerformanceCalculee> {
    const params = periode ? new HttpParams().set('periode', periode) : undefined;
    return this.http.get<PerformanceCalculee>(`${this.base}/calculees/me`, { params });
  }

  getRapportCsv(periode: string): Observable<Blob> {
    const params = new HttpParams().set('periode', periode);
    return this.http.get(`${this.base}/rapport`, { params, responseType: 'blob' });
  }

  getAnalyse(commercialId: number): Observable<AnalysePerformance> {
    const params = new HttpParams().set('commercialId', commercialId.toString());
    return this.http.get<AnalysePerformance>(`${this.base}/analyse`, { params });
  }

  getAnalyseMe(): Observable<AnalysePerformance> {
    return this.http.get<AnalysePerformance>(`${this.base}/analyse/me`);
  }
}
