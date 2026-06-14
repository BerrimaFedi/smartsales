package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import com.smartit.smartsales.dto.response.NotificationItem;
import com.smartit.smartsales.dto.response.NotificationResponse;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.UserRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calcule les notifications à la volée à partir des données existantes.
 * Aucune persistance, aucune tâche planifiée : chaque appel recalcule tout.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final VisiteRepository     visiteRepository;
    private final CommercialRepository commercialRepository;
    private final UserRepository       userRepository;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Point d'entrée principal : identifie l'utilisateur via le contexte de sécurité
     * et retourne la liste de notifications qui le concernent.
     */
    public NotificationResponse calculer() {
        String  username     = currentUsername();
        boolean isCommercial = isCommercialRole(username);

        List<Visite> visites = chargerVisites(username, isCommercial);
        if (visites == null) {
            // Commercial authentifié mais sans fiche Commercial associée
            return new NotificationResponse(0, List.of());
        }

        LocalDate     today     = LocalDate.now();
        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime finJour   = today.atTime(23, 59, 59);
        LocalDateTime maintenant = LocalDateTime.now();

        List<NotificationItem> notifications = new ArrayList<>();

        ajouterRappels  (notifications, visites, debutJour, finJour, isCommercial);
        ajouterRetards  (notifications, visites, debutJour, isCommercial);
        ajouterSuggestions(notifications, visites, maintenant, isCommercial);

        // Tri : warning (retards) en premier, puis ordre chronologique
        notifications.sort(
            Comparator.<NotificationItem, Integer>comparing(
                    n -> "warning".equals(n.severite()) ? 0 : 1)
                .thenComparing(NotificationItem::date)
        );

        return new NotificationResponse(notifications.size(), notifications);
    }

    // ── Chargement des visites selon le rôle ──────────────────────────────────

    /**
     * Retourne null si le commercial n'a pas de fiche associée (cas edge).
     */
    private List<Visite> chargerVisites(String username, boolean isCommercial) {
        if (isCommercial) {
            return commercialRepository.findByUserUsername(username)
                    .map(c -> visiteRepository.findByCommercialId(c.getId()))
                    .orElse(null);
        }
        // MANAGER / ADMIN : vue globale
        return visiteRepository.findAll();
    }

    // ── Calcul des trois types de notifications ────────────────────────────────

    /**
     * RAPPEL : visites PLANIFIEE prévues aujourd'hui.
     */
    private void ajouterRappels(List<NotificationItem> out, List<Visite> visites,
                                 LocalDateTime debutJour, LocalDateTime finJour,
                                 boolean isCommercial) {
        visites.stream()
                .filter(v -> v.getStatut() == StatutVisite.PLANIFIEE
                          && !v.getDateVisite().isBefore(debutJour)
                          && !v.getDateVisite().isAfter(finJour))
                .sorted(Comparator.comparing(Visite::getDateVisite))
                .forEach(v -> {
                    String client = nomClient(v);
                    String heure  = v.getDateVisite().format(FMT_TIME);
                    String msg    = isCommercial
                            ? "Visite chez " + client + " aujourd'hui à " + heure
                            : "Visite chez " + client
                              + " (" + nomCommercial(v) + ") aujourd'hui à " + heure;
                    out.add(new NotificationItem("RAPPEL", "Rappel de visite", msg,
                            v.getDateVisite(), "info"));
                });
    }

    /**
     * RETARD : visites PLANIFIEE ou EN_COURS dont la dateVisite est antérieure à aujourd'hui.
     */
    private void ajouterRetards(List<NotificationItem> out, List<Visite> visites,
                                 LocalDateTime debutJour, boolean isCommercial) {
        visites.stream()
                .filter(v -> (v.getStatut() == StatutVisite.PLANIFIEE
                           || v.getStatut() == StatutVisite.EN_COURS)
                          && v.getDateVisite().isBefore(debutJour))
                .sorted(Comparator.comparing(Visite::getDateVisite))
                .forEach(v -> {
                    String client  = nomClient(v);
                    String dateStr = v.getDateVisite().format(FMT_DATE);
                    String msg     = isCommercial
                            ? "Visite chez " + client + " du " + dateStr + " non terminée"
                            : "Visite chez " + client + " du " + dateStr
                              + " (" + nomCommercial(v) + ") non terminée";
                    out.add(new NotificationItem("RETARD", "Visite en retard", msg,
                            v.getDateVisite(), "warning"));
                });
    }

    /**
     * SUGGESTION : nombre de visites de type RELANCE non terminées (PLANIFIEE ou EN_COURS).
     * Génère une seule notification agrégée si au moins une relance est en attente.
     */
    private void ajouterSuggestions(List<NotificationItem> out, List<Visite> visites,
                                     LocalDateTime maintenant, boolean isCommercial) {
        long nbRelances = visites.stream()
                .filter(v -> v.getType() == TypeVisite.RELANCE
                          && (v.getStatut() == StatutVisite.PLANIFIEE
                           || v.getStatut() == StatutVisite.EN_COURS))
                .count();
        if (nbRelances > 0) {
            String msg = isCommercial
                    ? nbRelances + " client(s) à relancer"
                    : nbRelances + " relance(s) en attente pour l'équipe";
            out.add(new NotificationItem("SUGGESTION", "Relances en attente", msg,
                    maintenant, "info"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String nomClient(Visite v) {
        return v.getClient() != null ? v.getClient().getNom() : "Client inconnu";
    }

    private String nomCommercial(Visite v) {
        if (v.getCommercial() == null) return "N/A";
        return v.getCommercial().getNom() + " " + v.getCommercial().getPrenom();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isCommercialRole(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == Role.COMMERCIAL)
                .orElse(false);
    }
}
