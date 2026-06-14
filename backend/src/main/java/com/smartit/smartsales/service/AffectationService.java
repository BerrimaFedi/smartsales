package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.Zone;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import com.smartit.smartsales.dto.request.AffectationAutoRequest;
import com.smartit.smartsales.dto.request.ReaffectationRequest;
import com.smartit.smartsales.dto.response.AffectationEntryResponse;
import com.smartit.smartsales.dto.response.AffectationRapportResponse;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AffectationService {

    private final VisiteRepository visiteRepository;
    private final CommercialRepository commercialRepository;

    // -----------------------------------------------------------------------
    // PARTIE A — Affectation automatique
    // -----------------------------------------------------------------------

    /**
     * Prend toutes les visites PLANIFIEE non assignées sur la date (ou la liste d'IDs),
     * applique l'algorithme zone → charge → compétence négociation, et retourne le rapport.
     */
    public AffectationRapportResponse affecterAuto(AffectationAutoRequest request) {
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        // Récupération des visites à traiter
        List<Visite> cibles;
        if (request.visiteIds() != null && !request.visiteIds().isEmpty()) {
            cibles = visiteRepository.findByIdIn(request.visiteIds()).stream()
                    .filter(v -> v.getCommercial() == null && v.getStatut() == StatutVisite.PLANIFIEE)
                    .collect(Collectors.toList());
        } else {
            cibles = new ArrayList<>(visiteRepository.findByCommercialIsNullAndStatutAndDateVisiteBetween(
                    StatutVisite.PLANIFIEE, date.atStartOfDay(), date.atTime(LocalTime.MAX)));
        }

        List<Commercial> tousCommerciaux = commercialRepository.findAll();

        // Pré-calcul des charges (mis à jour au fil des affectations pour l'équilibrage incrémental)
        Map<Long, Long> chargeJour = chargeJour(date, tousCommerciaux);
        Map<Long, Long> chargeGlobale = chargeGlobale(tousCommerciaux);

        List<AffectationEntryResponse> affectees = new ArrayList<>();
        List<AffectationEntryResponse> nonAffectables = new ArrayList<>();

        for (Visite visite : cibles) {
            Resultat r = algorithmeDaffectation(visite, tousCommerciaux, chargeJour, chargeGlobale, Collections.emptySet());
            if (r.commercial() != null) {
                visite.setCommercial(r.commercial());
                visiteRepository.save(visite);
                // Mise à jour incrémentale des charges pour les visites suivantes
                chargeJour.merge(r.commercial().getId(), 1L, Long::sum);
                chargeGlobale.merge(r.commercial().getId(), 1L, Long::sum);
                affectees.add(toEntry(visite, r));
            } else {
                nonAffectables.add(toEntry(visite, r));
            }
        }

        return new AffectationRapportResponse(affectees, nonAffectables);
    }

    // -----------------------------------------------------------------------
    // PARTIE B — Réaffectation dynamique
    // -----------------------------------------------------------------------

    /**
     * Remet les visites PLANIFIEE/EN_COURS du commercial indisponible à l'état non assigné,
     * puis réapplique l'algorithme en excluant ce commercial des candidats.
     */
    public AffectationRapportResponse reaffecter(ReaffectationRequest request) {
        LocalDate debut = request.dateDebut() != null ? request.dateDebut() : LocalDate.now();
        LocalDate fin   = request.dateFin()   != null ? request.dateFin()   : debut.plusDays(30);

        // Récupérer ses visites PLANIFIEE + EN_COURS sur la période
        List<Visite> visitesDuCommercial = new ArrayList<>(
                visiteRepository.findByCommercialIdAndStatutInAndDateVisiteBetween(
                        request.commercialId(),
                        List.of(StatutVisite.PLANIFIEE, StatutVisite.EN_COURS),
                        debut.atStartOfDay(), fin.atTime(LocalTime.MAX)));

        // Remettre non assignées
        visitesDuCommercial.forEach(v -> v.setCommercial(null));
        visiteRepository.saveAll(visitesDuCommercial);

        // Candidats : tous sauf le commercial indisponible
        Set<Long> exclusions = Set.of(request.commercialId());
        List<Commercial> candidatsGlobaux = commercialRepository.findAll().stream()
                .filter(c -> !exclusions.contains(c.getId()))
                .collect(Collectors.toList());

        // Charge globale calculée une fois
        Map<Long, Long> chargeGlobale = chargeGlobale(candidatsGlobaux);

        List<AffectationEntryResponse> affectees = new ArrayList<>();
        List<AffectationEntryResponse> nonAffectables = new ArrayList<>();

        // Grouper par jour pour un calcul de charge journalier précis
        Map<LocalDate, List<Visite>> parJour = visitesDuCommercial.stream()
                .collect(Collectors.groupingBy(v -> v.getDateVisite().toLocalDate()));

        // Trier les jours pour un traitement déterministe
        parJour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate jour = entry.getKey();
                    Map<Long, Long> chargeJour = chargeJour(jour, candidatsGlobaux);

                    for (Visite visite : entry.getValue()) {
                        Resultat r = algorithmeDaffectation(visite, candidatsGlobaux, chargeJour, chargeGlobale, exclusions);
                        if (r.commercial() != null) {
                            visite.setCommercial(r.commercial());
                            visiteRepository.save(visite);
                            chargeJour.merge(r.commercial().getId(), 1L, Long::sum);
                            chargeGlobale.merge(r.commercial().getId(), 1L, Long::sum);
                            affectees.add(toEntry(visite, r));
                        } else {
                            nonAffectables.add(toEntry(visite, r));
                        }
                    }
                });

        return new AffectationRapportResponse(affectees, nonAffectables);
    }

    // -----------------------------------------------------------------------
    // Algorithme d'affectation (déterministe, commenté pour la soutenance)
    // -----------------------------------------------------------------------

    /**
     * Pour une visite donnée, choisit le meilleur commercial selon trois critères ordonnés :
     *
     * 1. Compétence NÉGOCIATION : si type == NEGOCIATION, ne garder que les commerciaux
     *    possédant une compétence dont le nom (normalisé, sans accents) contient "egocia".
     *    Si aucun négociateur n'existe → fallback sur tous les candidats.
     *
     * 2. Zone : privilégier les commerciaux dont la zone == zone du client.
     *    Si des candidats existent dans la bonne zone → ne garder qu'eux.
     *    Sinon → garder l'ensemble des candidats restants.
     *
     * 3. Charge journalière : parmi les finalistes, choisir celui qui a le moins de
     *    visites assignées ce jour-là. Égalité → charge globale → id (ordre stable).
     */
    private Resultat algorithmeDaffectation(
            Visite visite,
            List<Commercial> tousCommerciaux,
            Map<Long, Long> chargeJour,
            Map<Long, Long> chargeGlobale,
            Set<Long> exclusions) {

        List<Commercial> candidats = tousCommerciaux.stream()
                .filter(c -> !exclusions.contains(c.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (candidats.isEmpty()) {
            return new Resultat(null, "Aucun commercial disponible");
        }

        // — Étape 1 : contrainte compétence négociation —
        boolean contrainteNego = visite.getType() == TypeVisite.NEGOCIATION;
        boolean fallbackNego   = false;
        if (contrainteNego) {
            List<Commercial> negociateurs = candidats.stream()
                    .filter(this::estNegociateur)
                    .collect(Collectors.toList());
            if (!negociateurs.isEmpty()) {
                candidats = negociateurs;
            } else {
                fallbackNego = true; // aucun négociateur → on retombe sur tous
            }
        }

        // — Étape 2 : critère zone —
        Zone zoneClient = visite.getClient().getZone();
        boolean zoneRespectee = false;
        if (zoneClient != null) {
            List<Commercial> dansZone = candidats.stream()
                    .filter(c -> c.getZone() != null && c.getZone().getId().equals(zoneClient.getId()))
                    .collect(Collectors.toList());
            if (!dansZone.isEmpty()) {
                candidats = dansZone;
                zoneRespectee = true;
            }
        }

        // — Étape 3 : critère charge (min charge jour → min charge globale → id) —
        Commercial choisi = candidats.stream()
                .min(Comparator
                        .comparingLong((Commercial c) -> chargeJour.getOrDefault(c.getId(), 0L))
                        .thenComparingLong(c -> chargeGlobale.getOrDefault(c.getId(), 0L))
                        .thenComparingLong(Commercial::getId))
                .orElse(null);

        if (choisi == null) {
            return new Resultat(null, "Aucun commercial disponible après filtrage");
        }

        // — Construction de la raison (utile pour le rapport de soutenance) —
        StringBuilder raison = new StringBuilder();
        if (contrainteNego) {
            raison.append(fallbackNego
                    ? "NÉGOCIATION - aucun négociateur trouvé → fallback tous candidats; "
                    : "NÉGOCIATION - négociateur sélectionné; ");
        }
        if (zoneClient != null) {
            raison.append(zoneRespectee
                    ? "Zone respectée (" + zoneClient.getNom() + "); "
                    : "Aucun commercial dans la zone → étendu à tous; ");
        } else {
            raison.append("Client sans zone; ");
        }
        raison.append("Charge jour=").append(chargeJour.getOrDefault(choisi.getId(), 0L));

        return new Resultat(choisi, raison.toString());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Normalise une chaîne (minuscules, sans accents) pour la comparaison. */
    private String normaliser(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Un commercial est "négociateur" si une de ses compétences contient "egocia" (normalisé). */
    private boolean estNegociateur(Commercial c) {
        return c.getCompetences().stream()
                .anyMatch(comp -> normaliser(comp.getNom()).contains("egocia"));
    }

    /** Charge journalière : nb visites assignées (toutes) dans la journée, par commercial. */
    private Map<Long, Long> chargeJour(LocalDate date, List<Commercial> commerciaux) {
        Map<Long, Long> charge = new HashMap<>();
        for (Commercial c : commerciaux) {
            long nb = visiteRepository.countByCommercialIdAndDateVisiteBetween(
                    c.getId(), date.atStartOfDay(), date.atTime(LocalTime.MAX));
            charge.put(c.getId(), nb);
        }
        return charge;
    }

    /** Charge globale : nb total de visites assignées au commercial (toutes dates). */
    private Map<Long, Long> chargeGlobale(List<Commercial> commerciaux) {
        Map<Long, Long> charge = new HashMap<>();
        for (Commercial c : commerciaux) {
            charge.put(c.getId(), visiteRepository.countByCommercialId(c.getId()));
        }
        return charge;
    }

    private AffectationEntryResponse toEntry(Visite v, Resultat r) {
        return new AffectationEntryResponse(
                v.getId(),
                v.getClient().getNom(),
                v.getType(),
                r.commercial() != null ? r.commercial().getId()   : null,
                r.commercial() != null ? r.commercial().getNom() + " " + r.commercial().getPrenom() : null,
                r.raison()
        );
    }

    /** Résultat interne de l'algorithme pour une visite. */
    private record Resultat(Commercial commercial, String raison) {}
}
