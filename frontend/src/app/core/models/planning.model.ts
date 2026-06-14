import { Visite } from './visite.model';
import { TypeVisite } from './visite.model';

export interface PlanningResponse {
  visites: Visite[];
  distanceTotaleKm: number;
}

export interface TourneeDetail {
  ordre: number;
  visiteId: number;
  clientNom: string;
  distanceDepuisPrecedentKm: number;
}

export interface OptimizeResponse {
  ordre: number[];
  distanceTotaleKm: number;
  details: TourneeDetail[];
}

// --- Affectation automatique & réaffectation ---

export interface AffectationAutoRequest {
  date: string;          // YYYY-MM-DD
  visiteIds?: number[];  // optionnel : restreindre à ces IDs
}

export interface ReaffectationRequest {
  commercialId: number;
  dateDebut: string;  // YYYY-MM-DD
  dateFin?: string;   // YYYY-MM-DD (défaut : dateDebut + 30j côté backend)
}

export interface AffectationEntry {
  visiteId: number;
  clientNom: string;
  type: TypeVisite;
  commercialId: number | null;
  commercialNom: string | null;
  raison: string;
}

export interface AffectationRapport {
  affectees: AffectationEntry[];
  nonAffectables: AffectationEntry[];
}
