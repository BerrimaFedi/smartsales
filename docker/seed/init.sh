#!/bin/sh
# Seeder SmartSales
# - Attend que Hibernate ait créé les tables (polling sur la table "users")
# - Vérifie que les données ne sont pas déjà présentes (idempotence)
# - Charge smartsales-seed.sql
set -e

PSQL="psql -h postgres -U smartsales -d smartsales"

# 1. Attente que Hibernate crée la table ET que DataInitializer insère l'admin
echo "[seeder] Attente de l'utilisateur 'admin' (DataInitializer doit s'exécuter en premier)..."
WAITED=0
TIMEOUT=120
until PGPASSWORD=smartsales $PSQL -tAc "SELECT 1 FROM users WHERE username='admin' LIMIT 1" 2>/dev/null | grep -q 1; do
  if [ "$WAITED" -ge "$TIMEOUT" ]; then
    echo ""
    echo "[seeder] TIMEOUT : l'utilisateur admin n'est pas apparu après ${TIMEOUT}s. Abandon."
    exit 1
  fi
  printf '.'
  sleep 3
  WAITED=$((WAITED + 3))
done
echo ""

# 2. Vérification idempotence : si des zones existent déjà, le seed est ignoré
ZONE_COUNT=$(PGPASSWORD=smartsales $PSQL -tAc "SELECT COUNT(*) FROM zones;")
if [ "$ZONE_COUNT" -gt "0" ]; then
  echo "[seeder] Données déjà présentes ($ZONE_COUNT zones trouvées). Seed ignoré."
  exit 0
fi

# 3. Chargement du seed
echo "[seeder] Chargement du seed SQL..."
PGPASSWORD=smartsales $PSQL -f /seed.sql

echo "[seeder] Seed terminé avec succès."
