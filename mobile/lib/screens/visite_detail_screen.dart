import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/visite.dart';
import '../services/visite_service.dart';

/// Écran de détail d'une visite : check-in/check-out GPS, navigation, statut, compte-rendu.
class VisiteDetailScreen extends StatefulWidget {
  final Visite visite;
  const VisiteDetailScreen({super.key, required this.visite});

  @override
  State<VisiteDetailScreen> createState() => _VisiteDetailScreenState();
}

class _VisiteDetailScreenState extends State<VisiteDetailScreen> {
  final VisiteService _visiteService       = VisiteService();
  final _compteRenduController             = TextEditingController();
  final _montantController                 = TextEditingController();

  // Copie locale mutable de la visite pour rafraîchir l'écran sans pop
  late Visite _visite;
  late StatutVisite _statutSelectionne;

  bool _saving          = false;
  bool _checkInEnCours  = false;
  bool _checkOutEnCours = false;

  @override
  void initState() {
    super.initState();
    _visite            = widget.visite;
    _statutSelectionne = _visite.statut;
    _compteRenduController.text = _visite.compteRendu ?? '';
    if (_visite.montant != null) {
      _montantController.text = _visite.montant!.toStringAsFixed(2);
    }
  }

  @override
  void dispose() {
    _compteRenduController.dispose();
    _montantController.dispose();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Géolocalisation
  // ---------------------------------------------------------------------------

  /// Retourne la position GPS si disponible et autorisée, sinon (lat: null, lng: null).
  /// Ne plante jamais — le check-in se fait toujours, même sans coords.
  Future<({double? lat, double? lng})> _obtenirPosition() async {
    try {
      // Sur web, le service est toujours « actif » côté navigateur
      final serviceActif = await Geolocator.isLocationServiceEnabled();
      if (!serviceActif) return (lat: null, lng: null);

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) return (lat: null, lng: null);
      }
      if (permission == LocationPermission.deniedForever) return (lat: null, lng: null);

      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      ).timeout(const Duration(seconds: 15));
      return (lat: pos.latitude, lng: pos.longitude);
    } catch (_) {
      // Permission refusée, service indisponible, timeout ou erreur web → pas de coords
      return (lat: null, lng: null);
    }
  }

  // ---------------------------------------------------------------------------
  // Check-in
  // ---------------------------------------------------------------------------

  Future<void> _faireCheckIn() async {
    setState(() => _checkInEnCours = true);
    try {
      final pos     = await _obtenirPosition();
      final updated = await _visiteService.checkIn(
        _visite.id,
        lat: pos.lat,
        lng: pos.lng,
      );
      if (!mounted) return;
      setState(() {
        _visite            = updated;
        // Le backend passe la visite EN_COURS : on synchronise le chip sélectionné
        _statutSelectionne = updated.statut;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            pos.lat != null
                ? 'Check-in enregistré avec position GPS.'
                : 'Check-in enregistré (géolocalisation non disponible).',
          ),
          backgroundColor: Colors.green,
        ),
      );
    } on Exception catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content:         Text(e.toString().replaceFirst('Exception: ', '')),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _checkInEnCours = false);
    }
  }

  // ---------------------------------------------------------------------------
  // Check-out
  // ---------------------------------------------------------------------------

  Future<void> _faireCheckOut() async {
    setState(() => _checkOutEnCours = true);
    try {
      final pos     = await _obtenirPosition();
      final updated = await _visiteService.checkOut(
        _visite.id,
        lat: pos.lat,
        lng: pos.lng,
      );
      if (!mounted) return;
      setState(() => _visite = updated);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content:         Text('Check-out enregistré.'),
          backgroundColor: Colors.green,
        ),
      );
    } on Exception catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content:         Text(e.toString().replaceFirst('Exception: ', '')),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _checkOutEnCours = false);
    }
  }

  // ---------------------------------------------------------------------------
  // Navigation Google Maps
  // ---------------------------------------------------------------------------

  /// Ouvre Google Maps (itinéraire vers le client) dans un nouvel onglet sur web.
  Future<void> _ouvrirNavigation() async {
    final c = _visite.client;
    if (c?.latitude == null || c?.longitude == null) return;

    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1'
      '&destination=${c!.latitude},${c.longitude}',
    );
    // webOnlyWindowName: '_blank' ouvre un nouvel onglet sur Flutter web
    if (!await launchUrl(
      uri,
      mode:               LaunchMode.externalApplication,
      webOnlyWindowName:  '_blank',
    )) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Impossible d'ouvrir Google Maps.")),
        );
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Enregistrement statut / compte-rendu / montant (PATCH existant)
  // ---------------------------------------------------------------------------

  Future<void> _enregistrer() async {
    setState(() => _saving = true);

    final cr = _compteRenduController.text.trim();

    double? montant;
    if (_statutSelectionne == StatutVisite.terminee) {
      final raw = _montantController.text.trim().replaceAll(',', '.');
      if (raw.isNotEmpty) montant = double.tryParse(raw);
    }

    try {
      await _visiteService.updateVisite(
        _visite.id,
        statut:      _statutSelectionne,
        compteRendu: cr.isEmpty ? null : cr,
        montant:     montant,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content:         Text('Visite mise à jour avec succès.'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.of(context).pop();
    } on Exception catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content:         Text(e.toString().replaceFirst('Exception: ', '')),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /// Date et heure au format français : "3 juin 2025 à 09:30".
  String _formatDate(DateTime dt) {
    const mois = [
      '', 'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
      'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
    ];
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '${dt.day} ${mois[dt.month]} ${dt.year} à $h:$m';
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    final v                = _visite;
    final clientAvecCoords = v.client?.latitude != null && v.client?.longitude != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          v.client?.nom ?? 'Visite #${v.id}',
          overflow: TextOverflow.ellipsis,
        ),
        backgroundColor: Theme.of(context).primaryColor,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [

            // ----------------------------------------------------------------
            // Carte : informations générales de la visite
            // ----------------------------------------------------------------
            Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _InfoLigne(icone: Icons.business_outlined,
                               libelle: 'Client',  valeur: v.client?.nom ?? 'Non défini'),
                    if (v.client?.adresse.isNotEmpty == true)
                      _InfoLigne(icone: Icons.location_on_outlined,
                                 libelle: 'Adresse', valeur: v.client!.adresse),
                    _InfoLigne(icone: Icons.category_outlined,
                               libelle: 'Type',     valeur: v.type.libelle),
                    _InfoLigne(icone: Icons.calendar_today_outlined,
                               libelle: 'Date',     valeur: _formatDate(v.dateVisite)),
                    if (v.montant != null)
                      _InfoLigne(icone: Icons.euro_outlined,
                                 libelle: 'Montant', valeur: '${v.montant!.toStringAsFixed(2)} €'),
                    if (v.compteRendu != null && v.compteRendu!.isNotEmpty)
                      _InfoLigne(icone: Icons.notes_outlined,
                                 libelle: 'CR actuel', valeur: v.compteRendu!),

                    // Bouton de navigation GPS vers le client
                    if (clientAvecCoords) ...[
                      const SizedBox(height: 8),
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton.icon(
                          onPressed: _ouvrirNavigation,
                          icon:  const Icon(Icons.navigation_outlined),
                          label: const Text('Y aller'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: Theme.of(context).primaryColor,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // ----------------------------------------------------------------
            // Section Présence (check-in / check-out)
            // ----------------------------------------------------------------
            _SectionTitre('Présence'),
            const SizedBox(height: 10),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [

                    // Check-in : heure affichée si déjà fait, sinon bouton
                    if (v.checkIn != null)
                      _InfoLigne(
                        icone:   Icons.login_outlined,
                        libelle: 'Check-in',
                        valeur:  _formatDate(v.checkIn!),
                      )
                    else
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton.icon(
                          onPressed: _checkInEnCours ? null : _faireCheckIn,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.green,
                            foregroundColor: Colors.white,
                          ),
                          icon: _checkInEnCours
                              ? const SizedBox(
                                  width: 16, height: 16,
                                  child: CircularProgressIndicator(
                                    color: Colors.white, strokeWidth: 2),
                                )
                              : const Icon(Icons.login_outlined),
                          label: const Text('Check-in'),
                        ),
                      ),

                    const SizedBox(height: 8),

                    // Check-out : visible uniquement après le check-in
                    if (v.checkIn != null) ...[
                      if (v.checkOut != null)
                        _InfoLigne(
                          icone:   Icons.logout_outlined,
                          libelle: 'Check-out',
                          valeur:  _formatDate(v.checkOut!),
                        )
                      else
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton.icon(
                            onPressed: _checkOutEnCours ? null : _faireCheckOut,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.orange,
                              foregroundColor: Colors.white,
                            ),
                            icon: _checkOutEnCours
                                ? const SizedBox(
                                    width: 16, height: 16,
                                    child: CircularProgressIndicator(
                                      color: Colors.white, strokeWidth: 2),
                                  )
                                : const Icon(Icons.logout_outlined),
                            label: const Text('Check-out'),
                          ),
                        ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // ----------------------------------------------------------------
            // Changement de statut
            // ----------------------------------------------------------------
            _SectionTitre('Modifier le statut'),
            const SizedBox(height: 10),
            Wrap(
              spacing:    8,
              runSpacing: 4,
              children: StatutVisite.values.map((s) {
                final sel = s == _statutSelectionne;
                return ChoiceChip(
                  label:         Text(s.libelle),
                  selected:      sel,
                  selectedColor: s.couleur.withAlpha(55),
                  labelStyle: TextStyle(
                    color:      sel ? s.couleur : Colors.grey[700],
                    fontWeight: sel ? FontWeight.bold : FontWeight.normal,
                  ),
                  onSelected: (_) => setState(() => _statutSelectionne = s),
                );
              }).toList(),
            ),
            const SizedBox(height: 20),

            // ----------------------------------------------------------------
            // Montant (visible uniquement si statut TERMINEE)
            // ----------------------------------------------------------------
            if (_statutSelectionne == StatutVisite.terminee) ...[
              _SectionTitre('Montant (optionnel)'),
              const SizedBox(height: 10),
              TextField(
                controller:   _montantController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  hintText:   'Ex : 1500.00',
                  prefixIcon:  Icon(Icons.euro_outlined),
                  border:      OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 20),
            ],

            // ----------------------------------------------------------------
            // Compte-rendu
            // ----------------------------------------------------------------
            _SectionTitre('Compte-rendu'),
            const SizedBox(height: 10),
            TextField(
              controller: _compteRenduController,
              maxLines:   6,
              decoration: const InputDecoration(
                hintText:           'Résumé de la visite, points abordés, prochaines actions…',
                border:             OutlineInputBorder(),
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 28),

            // ----------------------------------------------------------------
            // Bouton Enregistrer (statut + compte-rendu + montant)
            // ----------------------------------------------------------------
            SizedBox(
              width:  double.infinity,
              height: 50,
              child: ElevatedButton.icon(
                onPressed: _saving ? null : _enregistrer,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).primaryColor,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                icon: _saving
                    ? const SizedBox(
                        width: 18, height: 18,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                      )
                    : const Icon(Icons.save_outlined),
                label: const Text('Enregistrer', style: TextStyle(fontSize: 16)),
              ),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widgets auxiliaires
// ---------------------------------------------------------------------------

/// Titre de section (Présence, Modifier le statut, etc.).
class _SectionTitre extends StatelessWidget {
  final String texte;
  const _SectionTitre(this.texte);

  @override
  Widget build(BuildContext context) {
    return Text(
      texte,
      style: Theme.of(context)
          .textTheme
          .titleMedium
          ?.copyWith(fontWeight: FontWeight.bold),
    );
  }
}

/// Ligne d'information avec icône, libellé court et valeur.
class _InfoLigne extends StatelessWidget {
  final IconData icone;
  final String   libelle;
  final String   valeur;
  const _InfoLigne({required this.icone, required this.libelle, required this.valeur});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icone, size: 18, color: Theme.of(context).primaryColor),
          const SizedBox(width: 8),
          SizedBox(
            width: 80,
            child: Text(
              libelle,
              style: const TextStyle(color: Colors.grey, fontSize: 13),
            ),
          ),
          Expanded(
            child: Text(
              valeur,
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
    );
  }
}
