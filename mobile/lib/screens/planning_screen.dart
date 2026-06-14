import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/visite.dart';
import '../providers/auth_provider.dart';
import '../services/notification_service.dart';
import '../services/visite_service.dart';
import 'assistant_screen.dart';
import 'login_screen.dart';
import 'notifications_screen.dart';
import 'visite_detail_screen.dart';

/// Écran principal du commercial : liste de ses visites groupées par jour.
class PlanningScreen extends StatefulWidget {
  const PlanningScreen({super.key});

  @override
  State<PlanningScreen> createState() => _PlanningScreenState();
}

class _PlanningScreenState extends State<PlanningScreen> {
  final VisiteService        _visiteService        = VisiteService();
  final NotificationService  _notificationService  = NotificationService();

  List<Visite>? _visites;
  String?       _erreur;
  bool          _chargement = true;
  int           _notifCount = 0; // count pour le badge de la cloche

  @override
  void initState() {
    super.initState();
    _chargerVisites();
    _chargerNotifCount();
  }

  /// Charge silencieusement le nombre de notifications pour afficher le badge.
  Future<void> _chargerNotifCount() async {
    try {
      final resp = await _notificationService.getNotifications();
      if (mounted) setState(() => _notifCount = resp.count);
    } catch (_) {
      // Silencieux : le badge reste à 0 en cas d'erreur réseau
    }
  }

  /// Ouvre l'écran des notifications et recharge le badge au retour.
  Future<void> _ouvrirNotifications() async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const NotificationsScreen()),
    );
    _chargerNotifCount();
  }

  /// Charge (ou recharge) les visites depuis l'API.
  Future<void> _chargerVisites() async {
    setState(() {
      _chargement = true;
      _erreur     = null;
    });
    try {
      final visites = await _visiteService.getMesVisites();
      // Tri chronologique croissant
      visites.sort((a, b) => a.dateVisite.compareTo(b.dateVisite));
      if (mounted) {
        setState(() {
          _visites    = visites;
          _chargement = false;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _erreur     = e.toString().replaceFirst('Exception: ', '');
          _chargement = false;
        });
      }
    }
  }

  Future<void> _deconnecter() async {
    await context.read<AuthProvider>().logout();
    if (mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      );
    }
  }

  // -------------------------------------------------------------------------
  // Helpers de formatage (sans dépendance intl)
  // -------------------------------------------------------------------------

  /// Regroupe les visites par date (clé = jour sans heure).
  Map<DateTime, List<Visite>> _grouperParJour(List<Visite> visites) {
    final Map<DateTime, List<Visite>> groupes = {};
    for (final v in visites) {
      final jour = DateTime(v.dateVisite.year, v.dateVisite.month, v.dateVisite.day);
      groupes.putIfAbsent(jour, () => []).add(v);
    }
    return groupes;
  }

  /// Libellé de section en français : "Aujourd'hui", "Demain" ou "3 juin 2025".
  String _libelleSection(DateTime jour) {
    final auj     = DateTime.now();
    final aujourd = DateTime(auj.year, auj.month, auj.day);
    final demain  = aujourd.add(const Duration(days: 1));
    if (jour == aujourd) return "Aujourd'hui";
    if (jour == demain)  return 'Demain';
    const mois = ['','janvier','février','mars','avril','mai','juin',
                   'juillet','août','septembre','octobre','novembre','décembre'];
    return '${jour.day} ${mois[jour.month]} ${jour.year}';
  }

  /// Heure au format HH:mm.
  String _heure(DateTime dt) =>
      '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';

  // -------------------------------------------------------------------------
  // Build
  // -------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mon planning'),
        backgroundColor: Theme.of(context).primaryColor,
        foregroundColor: Colors.white,
        actions: [
          // Cloche avec badge : notifications intelligentes
          IconButton(
            tooltip:  'Notifications',
            onPressed: _ouvrirNotifications,
            icon: Stack(
              clipBehavior: Clip.none,
              children: [
                const Icon(Icons.notifications_outlined),
                if (_notifCount > 0)
                  Positioned(
                    top:   -4,
                    right: -4,
                    child: Container(
                      constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                      padding: const EdgeInsets.symmetric(horizontal: 3),
                      decoration: const BoxDecoration(
                        color:  Colors.red,
                        shape:  BoxShape.circle,
                      ),
                      child: Text(
                        _notifCount > 99 ? '99+' : '$_notifCount',
                        style: const TextStyle(
                          color:      Colors.white,
                          fontSize:   9,
                          fontWeight: FontWeight.bold,
                          height:     1.6,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          // Accès rapide à l'assistant intelligent
          IconButton(
            icon:    const Icon(Icons.smart_toy_outlined),
            tooltip: 'Assistant',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const AssistantScreen()),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Déconnexion',
            onPressed: _deconnecter,
          ),
        ],
      ),
      body: _buildCorps(),
    );
  }

  Widget _buildCorps() {
    // État : chargement
    if (_chargement) {
      return const Center(child: CircularProgressIndicator());
    }

    // État : erreur
    if (_erreur != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 56, color: Colors.red),
              const SizedBox(height: 16),
              Text(_erreur!, textAlign: TextAlign.center),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _chargerVisites,
                icon: const Icon(Icons.refresh),
                label: const Text('Réessayer'),
              ),
            ],
          ),
        ),
      );
    }

    // État : liste vide
    final visites = _visites ?? [];
    if (visites.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.event_available, size: 72, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Aucune visite planifiée.',
              style: TextStyle(color: Colors.grey[600], fontSize: 16),
            ),
          ],
        ),
      );
    }

    // État : liste groupée avec pull-to-refresh
    final groupes = _grouperParJour(visites);
    final jours   = groupes.keys.toList()..sort();

    return RefreshIndicator(
      onRefresh: _chargerVisites,
      child: ListView.builder(
        padding: const EdgeInsets.only(bottom: 16),
        itemCount: jours.length,
        itemBuilder: (context, i) {
          final jour   = jours[i];
          final duJour = groupes[jour]!;
          return _SectionJour(
            titre:   _libelleSection(jour),
            visites: duJour,
            heure:   _heure,
            onTap: (visite) async {
              // On attend le retour du détail pour rafraîchir la liste
              await Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => VisiteDetailScreen(visite: visite),
                ),
              );
              _chargerVisites();
            },
          );
        },
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : section d'un jour
// ---------------------------------------------------------------------------

class _SectionJour extends StatelessWidget {
  final String                    titre;
  final List<Visite>              visites;
  final String Function(DateTime) heure;
  final void Function(Visite)     onTap;

  const _SectionJour({
    required this.titre,
    required this.visites,
    required this.heure,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // En-tête de section (date du jour)
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
          child: Text(
            titre,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: Theme.of(context).primaryColor,
            ),
          ),
        ),
        const Divider(height: 1, indent: 16, endIndent: 16),
        // Cartes des visites du jour
        ...visites.map((v) => _CarteVisite(visite: v, heure: heure, onTap: onTap)),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : carte d'une visite
// ---------------------------------------------------------------------------

class _CarteVisite extends StatelessWidget {
  final Visite                    visite;
  final String Function(DateTime) heure;
  final void Function(Visite)     onTap;

  const _CarteVisite({
    required this.visite,
    required this.heure,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statut = visite.statut;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: ListTile(
        onTap: () => onTap(visite),
        leading: CircleAvatar(
          backgroundColor: statut.couleur.withAlpha(40),
          child: Icon(Icons.business_outlined, color: statut.couleur, size: 20),
        ),
        title: Text(
          visite.client?.nom ?? 'Client non défini',
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        subtitle: Text(
          '${visite.type.libelle} · ${heure(visite.dateVisite)}',
          style: const TextStyle(fontSize: 13),
        ),
        trailing: _BadgeStatut(statut: statut),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : badge coloré de statut
// ---------------------------------------------------------------------------

class _BadgeStatut extends StatelessWidget {
  final StatutVisite statut;
  const _BadgeStatut({required this.statut});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color:        statut.couleur.withAlpha(30),
        border:       Border.all(color: statut.couleur.withAlpha(100)),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        statut.libelle,
        style: TextStyle(
          fontSize:   12,
          color:      statut.couleur,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
