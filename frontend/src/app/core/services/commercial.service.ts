import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Commercial, CommercialRequest } from '../models/commercial.model';

@Injectable({ providedIn: 'root' })
export class CommercialService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/commerciaux`;

  getAll(): Observable<Commercial[]> { return this.http.get<Commercial[]>(this.base); }
  getMe(): Observable<Commercial> { return this.http.get<Commercial>(`${this.base}/me`); }
  create(req: CommercialRequest): Observable<Commercial> { return this.http.post<Commercial>(this.base, req); }
  update(id: number, req: CommercialRequest): Observable<Commercial> { return this.http.put<Commercial>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
