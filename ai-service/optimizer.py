"""
Algorithme d'optimisation de tournées commerciales.
Implémentation pure Python : haversine + plus proche voisin + amélioration 2-opt.
"""

import math

EARTH_RADIUS_KM = 6371.0
# Réduction de coût (en km) accordée par niveau de priorité supérieur à 1.
# Ex : priorité 3 (NEGOCIATION) → 2 * (3-1) = 4 km de "rabais" fictif.
PRIORITY_DISCOUNT_KM = 2.0


# ---------------------------------------------------------------------------
# Distance
# ---------------------------------------------------------------------------

def haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Retourne la distance en km entre deux coordonnées GPS."""
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (math.sin(dlat / 2) ** 2
         + math.cos(math.radians(lat1))
         * math.cos(math.radians(lat2))
         * math.sin(dlon / 2) ** 2)
    return EARTH_RADIUS_KM * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def build_dist_matrix(points: list[dict]) -> list[list[float]]:
    """Construit la matrice symétrique des distances entre les points."""
    n = len(points)
    mat = [[0.0] * n for _ in range(n)]
    for i in range(n):
        for j in range(i + 1, n):
            d = haversine(points[i]["lat"], points[i]["lon"],
                          points[j]["lat"], points[j]["lon"])
            mat[i][j] = d
            mat[j][i] = d
    return mat


# ---------------------------------------------------------------------------
# Plus proche voisin avec pondération par priorité
# ---------------------------------------------------------------------------

def nearest_neighbor_tour(points: list[dict],
                           dist_matrix: list[list[float]],
                           from_depart: list[float] | None) -> list[int]:
    """
    Construit une tournée gloutonne depuis le point de départ.
    Pondération : coût_effectif = distance - PRIORITY_DISCOUNT_KM * (priorité - 1).
    Les visites de haute priorité semblent donc "plus proches".
    """
    n = len(points)
    visited = [False] * n

    # Choix du premier point
    if from_depart is not None:
        # Coût depuis le dépôt vers chaque point
        depart_costs = [
            haversine(from_depart[0], from_depart[1], p["lat"], p["lon"])
            - PRIORITY_DISCOUNT_KM * (p["priorite"] - 1)
            for p in points
        ]
        current = min(range(n), key=lambda i: depart_costs[i])
    else:
        # Pas de dépôt : on commence par la visite de plus haute priorité
        current = max(range(n), key=lambda i: points[i]["priorite"])

    route = [current]
    visited[current] = True

    for _ in range(n - 1):
        best_cost = math.inf
        best_next = -1
        for j in range(n):
            if visited[j]:
                continue
            cost = (dist_matrix[current][j]
                    - PRIORITY_DISCOUNT_KM * (points[j]["priorite"] - 1))
            if cost < best_cost:
                best_cost = cost
                best_next = j
        route.append(best_next)
        visited[best_next] = True
        current = best_next

    return route


# ---------------------------------------------------------------------------
# Amélioration 2-opt
# ---------------------------------------------------------------------------

def route_distance(route: list[int], dist_matrix: list[list[float]]) -> float:
    """Distance totale d'une route (boucle ouverte)."""
    return sum(dist_matrix[route[i]][route[i + 1]] for i in range(len(route) - 1))


def two_opt_improve(route: list[int], dist_matrix: list[list[float]]) -> list[int]:
    """
    Améliore la route par l'heuristique 2-opt.
    Échange les arêtes (i-1, i) et (j, j+1) si cela réduit la distance.
    """
    best = route[:]
    improved = True
    while improved:
        improved = False
        for i in range(1, len(best) - 1):
            for j in range(i + 1, len(best)):
                # Gain de l'échange : anciennes arêtes vs nouvelles
                gain = (dist_matrix[best[i - 1]][best[i]]
                        + dist_matrix[best[j]][best[j + 1] if j + 1 < len(best) else best[0]]
                        - dist_matrix[best[i - 1]][best[j]]
                        - dist_matrix[best[i]][best[j + 1] if j + 1 < len(best) else best[0]])
                if gain > 1e-9:
                    # Inversion du segment [i..j]
                    best[i:j + 1] = best[i:j + 1][::-1]
                    improved = True
    return best


# ---------------------------------------------------------------------------
# Point d'entrée principal
# ---------------------------------------------------------------------------

def optimize(visites_data: list[dict],
             depart_lat: float | None,
             depart_lon: float | None) -> tuple[list[int], float, list[dict]]:
    """
    Calcule la tournée optimale.

    Paramètres
    ----------
    visites_data : liste de dicts avec clés {id, lat, lon, priorite, client_nom, type}
    depart_lat, depart_lon : coordonnées du point de départ (None = non spécifié)

    Retour
    ------
    (ordre_ids, distance_totale_km, details)
    """
    n = len(visites_data)

    # --- Cas limites ---
    if n == 0:
        return [], 0.0, []

    if n == 1:
        p = visites_data[0]
        detail = {
            "ordre": 1,
            "visiteId": p["id"],
            "clientNom": p["client_nom"],
            "distanceDepuisPrecedentKm": 0.0,
        }
        return [p["id"]], 0.0, [detail]

    # Vérification des coordonnées : si l'un des points est invalide, ordre d'entrée
    for p in visites_data:
        if p["lat"] is None or p["lon"] is None:
            details = [
                {"ordre": i + 1, "visiteId": p["id"], "clientNom": p["client_nom"],
                 "distanceDepuisPrecedentKm": 0.0}
                for i, p in enumerate(visites_data)
            ]
            return [p["id"] for p in visites_data], 0.0, details

    # --- Algorithme principal ---
    dist_matrix = build_dist_matrix(visites_data)

    from_depart = (
        [depart_lat, depart_lon]
        if depart_lat is not None and depart_lon is not None
        else None
    )

    # 1. Tournée initiale par plus proche voisin avec priorité
    route_indices = nearest_neighbor_tour(visites_data, dist_matrix, from_depart)

    # 2. Amélioration 2-opt
    route_indices = two_opt_improve(route_indices, dist_matrix)

    # --- Calcul de la distance totale et des détails ---
    details = []
    total_km = 0.0

    for step, idx in enumerate(route_indices):
        point = visites_data[idx]
        if step == 0:
            # Distance depuis le dépôt (ou 0 si pas de dépôt)
            d = (haversine(from_depart[0], from_depart[1], point["lat"], point["lon"])
                 if from_depart else 0.0)
        else:
            prev_idx = route_indices[step - 1]
            d = dist_matrix[prev_idx][idx]

        total_km += d
        details.append({
            "ordre": step + 1,
            "visiteId": point["id"],
            "clientNom": point["client_nom"],
            "distanceDepuisPrecedentKm": round(d, 3),
        })

    ordre_ids = [visites_data[i]["id"] for i in route_indices]
    return ordre_ids, round(total_km, 3), details
