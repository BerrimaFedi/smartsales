package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import com.smartit.smartsales.dto.response.AssistantResponse;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.UserRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final VisiteRepository visiteRepository;
    private final CommercialRepository commercialRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AssistantResponse process(String message) {
        String username = currentUsername();
        boolean commercial = isCommercialRole(username);

        Long commercialId = null;
        String commercialNom = null;
        if (commercial) {
            Optional<Commercial> opt = commercialRepository.findByUserUsername(username);
            if (opt.isPresent()) {
                commercialId = opt.get().getId();
                commercialNom = opt.get().getNom() + " " + opt.get().getPrenom();
            }
        }

        String norm = normaliser(message);

        if (matchesVisitesJour(norm)) return handleVisitesJour(commercial, commercialId);
        if (matchesCA(norm))         return handleCA(commercial, commercialId, commercialNom);
        if (matchesTaux(norm))       return handleTaux(commercial, commercialId);
        if (matchesRelance(norm))    return handleRelance(commercial, commercialId);
        if (matchesProchaine(norm))  return handleProchaine(commercial, commercialId);
        if (matchesTerminees(norm))  return handleTerminees(commercial, commercialId);
        if (matchesAide(norm))       return handleAide(commercial);
        return handleFallback();
    }

    // ── Normalisation ────────────────────────────────────────────────────────

    private String normaliser(String message) {
        if (message == null) return "  ";
        String s = Normalizer.normalize(message.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return " " + s + " ";
    }

    private boolean has(String norm, String word) {
        return norm.contains(" " + word + " ");
    }

    // ── Intent detection ─────────────────────────────────────────────────────

    private boolean matchesVisitesJour(String m) {
        return has(m, "aujourd") || has(m, "journee")
                || (has(m, "visite") && has(m, "jour"))
                || has(m, "planning") || has(m, "mon planning");
    }

    private boolean matchesCA(String m) {
        return has(m, "chiffre") || has(m, "affaire")
                || has(m, "ca") || has(m, "revenu");
    }

    private boolean matchesTaux(String m) {
        return has(m, "taux") || has(m, "conversion");
    }

    private boolean matchesRelance(String m) {
        return has(m, "relancer") || has(m, "relance")
                || has(m, "rappeler") || has(m, "rappel");
    }

    private boolean matchesProchaine(String m) {
        return (has(m, "prochaine") || has(m, "prochain"))
                && has(m, "visite");
    }

    private boolean matchesTerminees(String m) {
        return has(m, "terminee") || has(m, "terminees")
                || (has(m, "combien") && has(m, "visite"))
                || has(m, "accomplies") || has(m, "termines");
    }

    private boolean matchesAide(String m) {
        return has(m, "aide") || has(m, "help")
                || has(m, "que peux") || has(m, "peux tu")
                || has(m, "fonctions") || has(m, "quoi")
                || has(m, "questions") || has(m, "comment");
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private AssistantResponse handleVisitesJour(boolean isCommercial, Long commercialId) {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);

        if (isCommercial) {
            List<Visite> visites = visiteRepository.findByCommercialIdAndDateVisiteBetween(commercialId, debut, fin);
            if (visites.isEmpty()) {
                return new AssistantResponse(
                        "Vous n'avez aucune visite planifiée pour aujourd'hui.",
                        List.of("Quels clients relancer ?", "Ma prochaine visite ?", "Mon CA ce mois ?"));
            }
            StringBuilder sb = new StringBuilder("Vous avez ")
                    .append(visites.size())
                    .append(visites.size() == 1 ? " visite" : " visites")
                    .append(" aujourd'hui :\n");
            visites.stream()
                    .sorted(Comparator.comparing(Visite::getDateVisite))
                    .forEach(v -> sb.append("• ")
                            .append(v.getDateVisite().format(FMT_DATETIME))
                            .append(" — ")
                            .append(v.getClient() != null ? v.getClient().getNom() : "Client inconnu")
                            .append(" (").append(formatType(v.getType())).append(")")
                            .append(" [").append(formatStatut(v.getStatut())).append("]\n"));
            return new AssistantResponse(sb.toString().trim(),
                    List.of("Ma prochaine visite ?", "Quels clients relancer ?", "Mon CA ce mois ?"));
        } else {
            List<Visite> visites = visiteRepository.findByDateVisiteBetween(debut, fin);
            return new AssistantResponse(
                    "L'équipe a " + visites.size() + " visite(s) planifiée(s) aujourd'hui.",
                    List.of("Quel est le CA global ?", "Quels clients relancer ?", "Mon taux de conversion ?"));
        }
    }

    private AssistantResponse handleCA(boolean isCommercial, Long commercialId, String commercialNom) {
        YearMonth mois = YearMonth.now();
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.atEndOfMonth().atTime(23, 59, 59);
        String periodeTxt = mois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));

        if (isCommercial) {
            List<Visite> visites = visiteRepository.findByCommercialIdAndDateVisiteBetween(commercialId, debut, fin);
            BigDecimal ca = visites.stream()
                    .filter(v -> v.getStatut() == StatutVisite.TERMINEE)
                    .map(v -> v.getMontant() != null ? v.getMontant() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long nbTerminees = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            return new AssistantResponse(
                    "Votre chiffre d'affaires pour " + periodeTxt + " est de " + ca.toPlainString()
                            + " DT (" + nbTerminees + " visite(s) terminée(s)).",
                    List.of("Mon taux de conversion ?", "Combien de visites terminées ?", "Quels clients relancer ?"));
        } else {
            List<Visite> visites = visiteRepository.findByDateVisiteBetween(debut, fin);
            BigDecimal ca = visites.stream()
                    .filter(v -> v.getStatut() == StatutVisite.TERMINEE)
                    .map(v -> v.getMontant() != null ? v.getMontant() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long nbTerminees = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            return new AssistantResponse(
                    "Le chiffre d'affaires global de l'équipe pour " + periodeTxt + " est de "
                            + ca.toPlainString() + " DT (" + nbTerminees + " visite(s) terminée(s)).",
                    List.of("Taux de conversion de l'équipe ?", "Visites d'aujourd'hui ?", "Clients à relancer ?"));
        }
    }

    private AssistantResponse handleTaux(boolean isCommercial, Long commercialId) {
        YearMonth mois = YearMonth.now();
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.atEndOfMonth().atTime(23, 59, 59);
        String periodeTxt = mois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));

        if (isCommercial) {
            List<Visite> visites = visiteRepository.findByCommercialIdAndDateVisiteBetween(commercialId, debut, fin);
            int total = visites.size();
            long terminees = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            double taux = total > 0 ? (double) terminees / total * 100 : 0.0;
            return new AssistantResponse(
                    String.format("Votre taux de conversion pour %s est de %.1f%% (%d visite(s) terminée(s) sur %d).",
                            periodeTxt, taux, terminees, total),
                    List.of("Mon CA ce mois ?", "Combien de visites terminées ?", "Quels clients relancer ?"));
        } else {
            List<Visite> visites = visiteRepository.findByDateVisiteBetween(debut, fin);
            int total = visites.size();
            long terminees = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            double taux = total > 0 ? (double) terminees / total * 100 : 0.0;
            return new AssistantResponse(
                    String.format("Le taux de conversion de l'équipe pour %s est de %.1f%% (%d/%d visites terminées).",
                            periodeTxt, taux, terminees, total),
                    List.of("CA global ?", "Visites d'aujourd'hui ?", "Clients à relancer ?"));
        }
    }

    private AssistantResponse handleRelance(boolean isCommercial, Long commercialId) {
        if (isCommercial) {
            List<Visite> relances = visiteRepository.findByCommercialId(commercialId).stream()
                    .filter(v -> v.getType() == TypeVisite.RELANCE
                            && (v.getStatut() == StatutVisite.PLANIFIEE || v.getStatut() == StatutVisite.EN_COURS))
                    .sorted(Comparator.comparing(Visite::getDateVisite))
                    .toList();
            if (relances.isEmpty()) {
                return new AssistantResponse(
                        "Vous n'avez aucune relance en attente. Continuez comme ça !",
                        List.of("Mes visites aujourd'hui ?", "Mon CA ce mois ?", "Ma prochaine visite ?"));
            }
            StringBuilder sb = new StringBuilder("Vous avez ")
                    .append(relances.size())
                    .append(relances.size() == 1 ? " relance" : " relances")
                    .append(" en attente :\n");
            relances.forEach(v -> sb.append("• ")
                    .append(v.getClient() != null ? v.getClient().getNom() : "Client inconnu")
                    .append(" — prévue le ")
                    .append(v.getDateVisite().format(FMT_DATE))
                    .append(" [").append(formatStatut(v.getStatut())).append("]\n"));
            return new AssistantResponse(sb.toString().trim(),
                    List.of("Mes visites aujourd'hui ?", "Ma prochaine visite ?", "Mon CA ce mois ?"));
        } else {
            List<Visite> relances = visiteRepository.findAll().stream()
                    .filter(v -> v.getType() == TypeVisite.RELANCE
                            && (v.getStatut() == StatutVisite.PLANIFIEE || v.getStatut() == StatutVisite.EN_COURS))
                    .sorted(Comparator.comparing(Visite::getDateVisite))
                    .toList();
            return new AssistantResponse(
                    "L'équipe a " + relances.size() + " relance(s) en attente au total.",
                    List.of("CA global ?", "Visites d'aujourd'hui ?", "Taux de conversion ?"));
        }
    }

    private AssistantResponse handleProchaine(boolean isCommercial, Long commercialId) {
        LocalDateTime maintenant = LocalDateTime.now();

        if (isCommercial) {
            Optional<Visite> prochaine = visiteRepository.findByCommercialId(commercialId).stream()
                    .filter(v -> v.getStatut() == StatutVisite.PLANIFIEE && v.getDateVisite().isAfter(maintenant))
                    .min(Comparator.comparing(Visite::getDateVisite));
            if (prochaine.isEmpty()) {
                return new AssistantResponse(
                        "Vous n'avez aucune visite planifiée à venir.",
                        List.of("Mes visites aujourd'hui ?", "Quels clients relancer ?", "Mon CA ce mois ?"));
            }
            Visite v = prochaine.get();
            String client = v.getClient() != null ? v.getClient().getNom() : "Client inconnu";
            return new AssistantResponse(
                    "Votre prochaine visite est chez " + client + " le "
                            + v.getDateVisite().format(FMT_DATETIME)
                            + " (" + formatType(v.getType()) + ").",
                    List.of("Mes visites aujourd'hui ?", "Quels clients relancer ?", "Mon CA ce mois ?"));
        } else {
            Optional<Visite> prochaine = visiteRepository.findAll().stream()
                    .filter(v -> v.getStatut() == StatutVisite.PLANIFIEE && v.getDateVisite().isAfter(maintenant))
                    .min(Comparator.comparing(Visite::getDateVisite));
            if (prochaine.isEmpty()) {
                return new AssistantResponse(
                        "Aucune visite planifiée à venir pour l'équipe.",
                        List.of("Visites d'aujourd'hui ?", "CA global ?", "Taux de conversion ?"));
            }
            Visite v = prochaine.get();
            String commercial = v.getCommercial() != null
                    ? v.getCommercial().getNom() + " " + v.getCommercial().getPrenom() : "N/A";
            String client = v.getClient() != null ? v.getClient().getNom() : "Client inconnu";
            return new AssistantResponse(
                    "La prochaine visite de l'équipe est celle de " + commercial + " chez " + client
                            + " le " + v.getDateVisite().format(FMT_DATETIME) + ".",
                    List.of("Visites d'aujourd'hui ?", "CA global ?", "Clients à relancer ?"));
        }
    }

    private AssistantResponse handleTerminees(boolean isCommercial, Long commercialId) {
        YearMonth mois = YearMonth.now();
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.atEndOfMonth().atTime(23, 59, 59);
        String periodeTxt = mois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));

        if (isCommercial) {
            List<Visite> visites = visiteRepository.findByCommercialIdAndDateVisiteBetween(commercialId, debut, fin);
            long count = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            return new AssistantResponse(
                    "Vous avez " + count + " visite(s) terminée(s) en " + periodeTxt + ".",
                    List.of("Mon CA ce mois ?", "Mon taux de conversion ?", "Quels clients relancer ?"));
        } else {
            List<Visite> visites = visiteRepository.findByDateVisiteBetween(debut, fin);
            long count = visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
            return new AssistantResponse(
                    "L'équipe a " + count + " visite(s) terminée(s) en " + periodeTxt + ".",
                    List.of("CA global ?", "Taux de conversion ?", "Clients à relancer ?"));
        }
    }

    private AssistantResponse handleAide(boolean isCommercial) {
        String intro = "Je peux répondre aux questions suivantes :\n";
        List<String> questions;
        if (isCommercial) {
            intro += "• « Quelles sont mes visites aujourd'hui ? »\n"
                    + "• « Quel est mon CA ce mois ? »\n"
                    + "• « Quel est mon taux de conversion ? »\n"
                    + "• « Quels clients relancer ? »\n"
                    + "• « Quelle est ma prochaine visite ? »\n"
                    + "• « Combien de visites terminées ce mois ? »";
            questions = List.of(
                    "Mes visites aujourd'hui ?",
                    "Mon CA ce mois ?",
                    "Quels clients relancer ?");
        } else {
            intro += "• « Combien de visites aujourd'hui ? »\n"
                    + "• « Quel est le CA global ? »\n"
                    + "• « Quel est le taux de conversion ? »\n"
                    + "• « Combien de relances en attente ? »\n"
                    + "• « Quelle est la prochaine visite ? »\n"
                    + "• « Combien de visites terminées ce mois ? »";
            questions = List.of(
                    "Visites d'aujourd'hui ?",
                    "CA global ?",
                    "Taux de conversion ?");
        }
        return new AssistantResponse(intro, questions);
    }

    private AssistantResponse handleFallback() {
        return new AssistantResponse(
                "Je n'ai pas compris votre question. Voici ce que je sais faire :",
                List.of("Aide — que peux-tu faire ?",
                        "Mes visites aujourd'hui ?",
                        "Mon CA ce mois ?"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String formatType(TypeVisite t) {
        if (t == null) return "—";
        return switch (t) {
            case PROSPECTION -> "Prospection";
            case RELANCE -> "Relance";
            case NEGOCIATION -> "Négociation";
        };
    }

    private String formatStatut(StatutVisite s) {
        if (s == null) return "—";
        return switch (s) {
            case PLANIFIEE -> "Planifiée";
            case EN_COURS -> "En cours";
            case TERMINEE -> "Terminée";
            case ANNULEE -> "Annulée";
        };
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
