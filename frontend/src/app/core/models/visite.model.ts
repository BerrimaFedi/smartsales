export enum TypeVisite {
  PROSPECTION = 'PROSPECTION',
  RELANCE     = 'RELANCE',
  NEGOCIATION = 'NEGOCIATION',
}

export enum StatutVisite {
  PLANIFIEE = 'PLANIFIEE',
  EN_COURS  = 'EN_COURS',
  TERMINEE  = 'TERMINEE',
  ANNULEE   = 'ANNULEE',
}

export interface ClientInfo {
  id: number;
  nom: string;
  adresse: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface Visite {
  id: number;
  commercialId: number | null;
  commercialNom: string | null;
  clientId: number;
  clientNom: string;
  client?: ClientInfo;
  dateVisite: string;
  type: TypeVisite;
  statut: StatutVisite;
  compteRendu: string | null;
  ordreTournee: number | null;
  checkIn: string | null;
  checkOut: string | null;
  montant: number;
}

export interface VisiteRequest {
  commercialId: number | null;
  clientId: number;
  dateVisite: string;
  type: TypeVisite;
  statut?: StatutVisite;
  compteRendu?: string;
  ordreTournee?: number;
  montant?: number;
}

export interface VisitePatchRequest {
  statut?: StatutVisite;
  compteRendu?: string | null;
  montant?: number | null;
}
