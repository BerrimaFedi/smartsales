import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Visite, VisitePatchRequest, VisiteRequest } from '../models/visite.model';

@Injectable({ providedIn: 'root' })
export class VisiteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/visites`;

  getAll(): Observable<Visite[]> { return this.http.get<Visite[]>(this.base); }
  create(req: VisiteRequest): Observable<Visite> { return this.http.post<Visite>(this.base, req); }
  update(id: number, req: VisiteRequest): Observable<Visite> { return this.http.put<Visite>(`${this.base}/${id}`, req); }
  patch(id: number, req: VisitePatchRequest): Observable<Visite> { return this.http.patch<Visite>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
