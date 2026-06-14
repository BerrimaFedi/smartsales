export interface Performance {
  id: number;
  commercialId: number;
  commercialNom: string;
  periode: string;
  chiffreAffaires: number;
  nombreVisites: number;
  tauxConversion: number;
}

export interface PerformanceRequest {
  commercialId: number;
  periode: string;
  chiffreAffaires?: number;
  nombreVisites?: number;
  tauxConversion?: number;
}

export interface PerformanceCalculee {
  commercialId: number;
  commercialNom: string;
  periode: string;
  chiffreAffaires: number;
  nombreVisites: number;
  nombreVisitesTerminees: number;
  tauxConversion: number;
  isManuel: boolean;
  performanceManuelleId: number | null;
}

export interface AnalysePerformance {
  commercialId: number;
  commercialNom: string;
  tendanceCA: 'en hausse' | 'stable' | 'en baisse' | 'données insuffisantes';
  variationCAPct: number | null;
  tauxConversionMoyen: number;
  anomalieDetectee: boolean;
  anomalieDescription: string | null;
  recommandations: string[];
}
