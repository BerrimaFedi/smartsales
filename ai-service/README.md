# SmartSales — Service IA

Service FastAPI d'optimisation des tournées commerciales.

## Lancement

```bash
# Installer les dépendances
pip install -r requirements.txt

# Démarrer le serveur (mode développement)
uvicorn main:app --reload --port 8000
```

Documentation interactive disponible sur : http://localhost:8000/docs

## Endpoints

| Méthode | URL        | Description                          |
|---------|------------|--------------------------------------|
| GET     | /health    | Santé du service                     |
| POST    | /optimize  | Optimise l'ordre d'une tournée       |

## Algorithme

1. **Distance** : formule de Haversine (km entre deux points GPS)
2. **Tournée initiale** : plus proche voisin avec pondération par priorité  
   (les visites de type NEGOCIATION sont favorisées par un rabais fictif de distance)
3. **Amélioration** : heuristique 2-opt (échanges d'arêtes réduisant la distance totale)

## Exemple

```bash
curl -X POST http://localhost:8000/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "departLatitude": 36.8190,
    "departLongitude": 10.1658,
    "visites": [
      {"id": 1, "clientNom": "Société Alpha", "latitude": 36.8065, "longitude": 10.1815, "type": "PROSPECTION", "priorite": 1},
      {"id": 2, "clientNom": "Société Beta",  "latitude": 36.8448, "longitude": 10.1940, "type": "NEGOCIATION",  "priorite": 3},
      {"id": 3, "clientNom": "Société Gamma", "latitude": 36.8272, "longitude": 10.1658, "type": "RELANCE",      "priorite": 2}
    ]
  }'
```
