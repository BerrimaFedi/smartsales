"""
SmartSales — Service IA : optimisation des tournées + analyse des performances.
FastAPI · Python 3.11 · Port 8000
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from optimizer import optimize
from analyzer import analyser_commercial

app = FastAPI(
    title="SmartSales AI Service",
    description="Optimisation des tournées commerciales (plus proche voisin + 2-opt)",
    version="1.0.0",
)


# ---------------------------------------------------------------------------
# Modèles Pydantic
# ---------------------------------------------------------------------------

class VisitePoint(BaseModel):
    id: int
    clientNom: str
    latitude: float
    longitude: float
    type: str
    priorite: int = Field(default=1, ge=1, le=10)


class OptimizeRequest(BaseModel):
    departLatitude: float | None = None
    departLongitude: float | None = None
    visites: list[VisitePoint]


class TourneeDetail(BaseModel):
    ordre: int
    visiteId: int
    clientNom: str
    distanceDepuisPrecedentKm: float


class OptimizeResponse(BaseModel):
    ordre: list[int]
    distanceTotaleKm: float
    details: list[TourneeDetail]


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health", tags=["monitoring"])
async def health():
    """Vérifie que le service est opérationnel."""
    return {"status": "ok"}


# ---------------------------------------------------------------------------
# Modèles Pydantic — analyse des performances
# ---------------------------------------------------------------------------

class PerformanceHistoriqueItem(BaseModel):
    """Une période de performance pour un commercial."""
    periode: str
    chiffreAffaires: float
    nombreVisites: int
    nombreVisitesTerminees: int
    tauxConversion: float  # entre 0 et 1


class CommercialHistorique(BaseModel):
    """Historique complet d'un commercial (toutes périodes)."""
    commercialId: int
    commercialNom: str
    historique: list[PerformanceHistoriqueItem]


class AnalyseRequest(BaseModel):
    """Corps de la requête POST /analyze-performances."""
    commerciaux: list[CommercialHistorique]


class AnalyseResultat(BaseModel):
    """Résultat de l'analyse pour un commercial."""
    commercialId: int
    commercialNom: str
    tendanceCA: str
    variationCAPct: float | None
    tauxConversionMoyen: float
    anomalieDetectee: bool
    anomalieDescription: str | None
    recommandations: list[str]


class AnalyseResponse(BaseModel):
    """Corps de la réponse POST /analyze-performances."""
    analyses: list[AnalyseResultat]


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.post("/analyze-performances", response_model=AnalyseResponse, tags=["analyse"])
async def analyze_performances(request: AnalyseRequest):
    """
    Analyse statistique des performances commerciales.

    - Tendance CA : compare la dernière période à la moyenne des périodes précédentes.
    - Anomalie : détection si CA ou taux < moyenne − 1 écart-type (min 3 périodes précédentes).
    - Recommandations : règles métier déterministes basées sur les indicateurs calculés.
    """
    analyses: list[AnalyseResultat] = []

    for commercial in request.commerciaux:
        # Conversion des modèles Pydantic en dicts pour l'analyseur
        historique_data = [item.model_dump() for item in commercial.historique]
        try:
            resultat = analyser_commercial(historique_data)
        except Exception as exc:
            # Sécurité : ne jamais laisser une erreur interne casser l'API
            raise HTTPException(status_code=500, detail=f"Erreur d'analyse pour {commercial.commercialNom} : {exc}")

        analyses.append(AnalyseResultat(
            commercialId=commercial.commercialId,
            commercialNom=commercial.commercialNom,
            **resultat,
        ))

    return AnalyseResponse(analyses=analyses)


@app.post("/optimize", response_model=OptimizeResponse, tags=["optimisation"])
async def optimize_route(request: OptimizeRequest):
    """
    Calcule l'ordre optimal des visites pour une tournée commerciale.

    - Algorithme : plus proche voisin (avec pondération par priorité) + amélioration 2-opt.
    - Priorité : NEGOCIATION=3 > RELANCE=2 > PROSPECTION=1 (défini côté backend Java).
    - Robustesse : 0 ou 1 visite, ou coordonnées manquantes → ordre d'entrée retourné sans erreur.
    """
    # Conversion des modèles Pydantic en dicts simples pour l'optimiseur
    visites_data = [
        {
            "id": v.id,
            "lat": v.latitude,
            "lon": v.longitude,
            "priorite": v.priorite,
            "client_nom": v.clientNom,
            "type": v.type,
        }
        for v in request.visites
    ]

    try:
        ordre, distance, details_raw = optimize(
            visites_data,
            request.departLatitude,
            request.departLongitude,
        )
    except Exception as exc:
        # Ne jamais laisser une erreur interne casser l'API
        raise HTTPException(status_code=500, detail=f"Erreur d'optimisation : {exc}")

    details = [TourneeDetail(**d) for d in details_raw]
    return OptimizeResponse(ordre=ordre, distanceTotaleKm=distance, details=details)
