import { Zone } from './zone.model';

export interface Client {
  id: number;
  nom: string;
  adresse: string | null;
  telephone: string | null;
  latitude: number | null;
  longitude: number | null;
  zone: Zone | null;
}

export interface ClientRequest {
  nom: string;
  adresse?: string;
  telephone?: string;
  latitude?: number;
  longitude?: number;
  zoneId?: number;
}
