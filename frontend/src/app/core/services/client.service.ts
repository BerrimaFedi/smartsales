import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Client, ClientRequest } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/clients`;

  getAll(): Observable<Client[]> { return this.http.get<Client[]>(this.base); }
  create(req: ClientRequest): Observable<Client> { return this.http.post<Client>(this.base, req); }
  update(id: number, req: ClientRequest): Observable<Client> { return this.http.put<Client>(`${this.base}/${id}`, req); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
