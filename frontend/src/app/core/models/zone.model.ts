export interface Zone {
  id: number;
  nom: string;
  description: string | null;
}

export interface ZoneRequest {
  nom: string;
  description?: string;
}
