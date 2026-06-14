import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Competence, CompetenceRequest } from '../models/competence.model';

@Injectable({ providedIn: 'root' })
export class CompetenceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/competences`;

  getAll(): Observable<Competence[]>                          { return this.http.get<Competence[]>(this.base); }
  create(req: CompetenceRequest): Observable<Competence>     { return this.http.post<Competence>(this.base, req); }
  update(id: number, req: CompetenceRequest): Observable<Competence> { return this.http.put<Competence>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void>                       { return this.http.delete<void>(`${this.base}/${id}`); }
}
