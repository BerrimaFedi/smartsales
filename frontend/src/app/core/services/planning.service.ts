import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AffectationAutoRequest,
  AffectationRapport,
  PlanningResponse,
  ReaffectationRequest,
} from '../models/planning.model';

@Injectable({ providedIn: 'root' })
export class PlanningService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/planning`;

  optimiser(commercialId: number, date: string): Observable<PlanningResponse> {
    return this.http.post<PlanningResponse>(
      `${this.base}/optimiser?commercialId=${commercialId}&date=${date}`,
      {}
    );
  }

  affecterAuto(req: AffectationAutoRequest): Observable<AffectationRapport> {
    return this.http.post<AffectationRapport>(`${this.base}/affecter-auto`, req);
  }

  reaffecter(req: ReaffectationRequest): Observable<AffectationRapport> {
    return this.http.post<AffectationRapport>(`${this.base}/reaffecter`, req);
  }
}
