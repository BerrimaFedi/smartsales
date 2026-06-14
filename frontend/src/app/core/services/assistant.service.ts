import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssistantRequest, AssistantResponse } from '../models/assistant.model';

@Injectable({ providedIn: 'root' })
export class AssistantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/assistant`;

  chat(message: string): Observable<AssistantResponse> {
    const req: AssistantRequest = { message };
    return this.http.post<AssistantResponse>(this.base, req);
  }
}
