import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Zone, ZoneRequest } from '../models/zone.model';

@Injectable({ providedIn: 'root' })
export class ZoneService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/zones`;

  getAll(): Observable<Zone[]> { return this.http.get<Zone[]>(this.base); }
  create(req: ZoneRequest): Observable<Zone> { return this.http.post<Zone>(this.base, req); }
  update(id: number, req: ZoneRequest): Observable<Zone> { return this.http.put<Zone>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
