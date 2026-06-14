export interface CaParCommercial {
  commercial: string;
  ca: number;
}

export interface CaParMois {
  /** Format "AAAA-MM", ex : "2025-06" */
  periode: string;
  ca: number;
}

export interface VisitesParStatut {
  planifiee: number;
  enCours:   number;
  terminee:  number;
  annulee:   number;
}

export interface VisitesParType {
  prospection: number;
  relance:     number;
  negociation: number;
}

export interface DashboardStats {
  caParCommercial:  CaParCommercial[];
  visitesParStatut: VisitesParStatut;
  visitesParType:   VisitesParType;
  caParMois:        CaParMois[];
}
