"""
SmartSales — Module d'analyse statistique des performances commerciales.
Algorithme déterministe (sans LLM) : tendance par comparaison à la moyenne,
anomalie via moyenne − écart-type, recommandations par règles métier.
"""

import math
from typing import Optional

# ---------------------------------------------------------------------------
# Seuils configurables (ajuster selon le contexte métier)
# ---------------------------------------------------------------------------

# En dessous de ce taux de conversion → recommander la qualification des visites
SEUIL_TAUX_CONVERSION_FAIBLE: float = 0.20

# Si plus de ce ratio des visites ne sont pas terminées → recommander le suivi
SEUIL_RATIO_NON_TERMINEES: float = 0.60

# Facteur d'écart-type : CA < moyenne − FACTEUR × σ → baisse anormale
FACTEUR_ANOMALIE: float = 1.0

# Nombre minimum de périodes précédentes pour calculer une tendance fiable
MIN_PERIODES_TENDANCE: int = 2

# Nombre minimum de périodes précédentes pour détecter une anomalie (σ peu fiable sinon)
MIN_PERIODES_ANOMALIE: int = 3

# Variation (%) au-delà de laquelle on parle de « hausse » ou « baisse »
SEUIL_VARIATION_PCT: float = 5.0


# ---------------------------------------------------------------------------
# Fonction principale
# ---------------------------------------------------------------------------

def analyser_commercial(historique: list[dict]) -> dict:
    """
    Analyse les performances d'un commercial à partir de son historique.

    Paramètre — liste de dicts avec les clés :
      periode (str AAAA-MM), chiffreAffaires (float), nombreVisites (int),
      nombreVisitesTerminees (int), tauxConversion (float, entre 0 et 1).

    Retour — dict avec :
      tendanceCA          : "en hausse" | "stable" | "en baisse" | "données insuffisantes"
      variationCAPct      : float ou None (% de variation vs moyenne historique)
      tauxConversionMoyen : float (en %)
      anomalieDetectee    : bool
      anomalieDescription : str ou None
      recommandations     : list[str]
    """
    # Tri chronologique garanti
    historique = sorted(historique, key=lambda x: x["periode"])
    n = len(historique)

    if n == 0:
        return _resultat_vide("Aucune donnée disponible")

    derniere = historique[-1]
    precedentes = historique[:-1]

    # --- Tendance sur le CA ---
    tendance_ca, variation_ca_pct = _calculer_tendance(
        float(derniere["chiffreAffaires"]),
        [float(p["chiffreAffaires"]) for p in precedentes],
    )

    # --- Taux de conversion moyen sur toutes les périodes ---
    taux_moyen = sum(float(p["tauxConversion"]) for p in historique) / n

    # --- Détection d'anomalie sur le CA ---
    anomalie_ca, desc_ca = _detecter_anomalie(
        float(derniere["chiffreAffaires"]),
        [float(p["chiffreAffaires"]) for p in precedentes],
        "CA",
    )

    # --- Détection d'anomalie sur le taux de conversion ---
    anomalie_taux, desc_taux = _detecter_anomalie(
        float(derniere["tauxConversion"]),
        [float(p["tauxConversion"]) for p in precedentes],
        "taux de conversion",
    )

    anomalie_detectee = anomalie_ca or anomalie_taux
    descriptions = [d for d in [desc_ca, desc_taux] if d]
    anomalie_description = " | ".join(descriptions) if descriptions else None

    recommandations = _generer_recommandations(derniere, tendance_ca, taux_moyen, anomalie_ca)

    return {
        "tendanceCA": tendance_ca,
        "variationCAPct": round(variation_ca_pct, 2) if variation_ca_pct is not None else None,
        "tauxConversionMoyen": round(taux_moyen * 100, 1),
        "anomalieDetectee": anomalie_detectee,
        "anomalieDescription": anomalie_description,
        "recommandations": recommandations,
    }


# ---------------------------------------------------------------------------
# Calcul de tendance
# ---------------------------------------------------------------------------

def _calculer_tendance(
    valeur: float, precedentes: list[float]
) -> tuple[str, Optional[float]]:
    """
    Compare la valeur de la dernière période à la moyenne des précédentes.
    Retourne (label_tendance, pourcentage_variation).
    Renvoie "données insuffisantes" si moins de MIN_PERIODES_TENDANCE périodes précédentes.
    """
    if len(precedentes) < MIN_PERIODES_TENDANCE:
        return "données insuffisantes", None

    moyenne = sum(precedentes) / len(precedentes)

    if moyenne == 0:
        return ("en hausse", None) if valeur > 0 else ("stable", 0.0)

    variation = (valeur - moyenne) / moyenne * 100

    if variation > SEUIL_VARIATION_PCT:
        return "en hausse", variation
    elif variation < -SEUIL_VARIATION_PCT:
        return "en baisse", variation
    else:
        return "stable", variation


# ---------------------------------------------------------------------------
# Détection d'anomalie (baisse anormale via écart-type)
# ---------------------------------------------------------------------------

def _detecter_anomalie(
    valeur: float, precedentes: list[float], label: str
) -> tuple[bool, Optional[str]]:
    """
    Détecte une baisse anormale si valeur < moyenne − FACTEUR × σ.
    Nécessite MIN_PERIODES_ANOMALIE périodes précédentes pour que σ soit significatif.
    """
    if len(precedentes) < MIN_PERIODES_ANOMALIE:
        return False, None

    n = len(precedentes)
    moyenne = sum(precedentes) / n
    # Variance population (pas d'estimation — on a l'historique complet)
    variance = sum((x - moyenne) ** 2 for x in precedentes) / n
    ecart_type = math.sqrt(variance)

    seuil = moyenne - FACTEUR_ANOMALIE * ecart_type

    if valeur < seuil:
        desc = (
            f"Baisse anormale du {label} : valeur actuelle {valeur:.2f} "
            f"< seuil {seuil:.2f} (moyenne historique {moyenne:.2f})"
        )
        return True, desc

    return False, None


# ---------------------------------------------------------------------------
# Génération de recommandations (règles métier simples)
# ---------------------------------------------------------------------------

def _generer_recommandations(
    derniere: dict,
    tendance_ca: str,
    taux_moyen: float,
    anomalie_ca: bool,
) -> list[str]:
    """
    Génère des recommandations actionnables selon des règles métier déterministes.
    Chaque règle est indépendante ; plusieurs peuvent s'appliquer simultanément.
    """
    recommandations: list[str] = []

    nb_visites = int(derniere.get("nombreVisites", 0))
    nb_terminees = int(derniere.get("nombreVisitesTerminees", 0))
    taux_actuel = float(derniere.get("tauxConversion", 0))

    # Règle 1 — taux de conversion faible sur la dernière période
    if taux_actuel < SEUIL_TAUX_CONVERSION_FAIBLE:
        recommandations.append(
            f"Taux de conversion faible ({taux_actuel * 100:.1f}%) : "
            "améliorer la qualification des visites et le ciblage des prospects."
        )

    # Règle 2 — beaucoup de visites non terminées (suivi insuffisant)
    if nb_visites > 0:
        ratio_non_terminees = (nb_visites - nb_terminees) / nb_visites
        if ratio_non_terminees > SEUIL_RATIO_NON_TERMINEES:
            recommandations.append(
                f"Trop de visites non converties ({nb_visites - nb_terminees}/{nb_visites}) : "
                "prioriser le suivi des rendez-vous en cours."
            )

    # Règle 3 — CA en baisse ou anomalie détectée
    if tendance_ca == "en baisse" or anomalie_ca:
        recommandations.append(
            "CA en baisse : relancer les clients dormants et revoir le portefeuille de prospects."
        )

    # Règle 4 — bonnes performances → encouragement et partage
    if tendance_ca == "en hausse" and taux_actuel >= SEUIL_TAUX_CONVERSION_FAIBLE:
        recommandations.append(
            "Bonnes performances : capitaliser sur les pratiques actuelles "
            "et partager les bonnes pratiques avec l'équipe."
        )

    if not recommandations:
        recommandations.append("Performances dans les normes : maintenir le rythme actuel.")

    return recommandations


# ---------------------------------------------------------------------------
# Utilitaire
# ---------------------------------------------------------------------------

def _resultat_vide(raison: str) -> dict:
    return {
        "tendanceCA": "données insuffisantes",
        "variationCAPct": None,
        "tauxConversionMoyen": 0.0,
        "anomalieDetectee": False,
        "anomalieDescription": raison,
        "recommandations": ["Données insuffisantes pour générer des recommandations."],
    }
