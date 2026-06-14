import { Zone } from './zone.model';
import { Competence } from './competence.model';

export interface Commercial {
  id: number;
  nom: string;
  prenom: string;
  telephone: string | null;
  username: string | null;
  email: string | null;
  zone: Zone | null;
  competences: Competence[];
}

export interface CommercialRequest {
  nom: string;
  prenom: string;
  telephone?: string;
  zoneId?: number;
  competenceIds?: number[];
  username: string;
  email: string;
  password?: string;  // obligatoire à la création, omis ou vide à l'édition → inchangé
}
