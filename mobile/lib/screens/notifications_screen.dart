import 'package:flutter/material.dart';
import '../models/notification.dart';
import '../services/notification_service.dart';

/// Couleur principale de l'app (thème bleu SmartSales).
const Color _bleuPrincipal = Color(0xFF1565C0);

/// Écran de liste des notifications intelligentes.
///
/// Recharge automatiquement les données à chaque ouverture.
/// RETARD affiché en orange, RAPPEL et SUGGESTION en bleu.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final NotificationService _service = NotificationService();

  List<NotificationItem> _items   = [];
  bool                   _loading = true;
  String?                _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  /// Charge les notifications depuis l'API.
  Future<void> _charger() async {
    setState(() {
      _loading = true;
      _erreur  = null;
    });
    try {
      final resp = await _service.getNotifications();
      if (mounted) {
        setState(() {
          _items   = resp.notifications;
          _loading = false;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _erreur  = e.toString().replaceFirst('Exception: ', '');
          _loading = false;
        });
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage selon le type / sévérité
  // ---------------------------------------------------------------------------

  /// Icône Material associée au type de notification.
  IconData _icone(String type) {
    switch (type) {
      case 'RETARD':     return Icons.warning_amber_rounded;
      case 'RAPPEL':     return Icons.notifications_outlined;
      case 'SUGGESTION': return Icons.lightbulb_outline;
      default:           return Icons.info_outline;
    }
  }

  /// Couleur de l'icône et de l'accent selon la sévérité.
  Color _couleur(String severite) =>
      severite == 'warning' ? Colors.orange.shade700 : _bleuPrincipal;

  /// Format de date court : "14/06/2026 09:30".
  String _formatDate(DateTime dt) {
    final j = dt.day.toString().padLeft(2, '0');
    final m = dt.month.toString().padLeft(2, '0');
    final h = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    return '$j/$m/${dt.year} $h:$min';
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        backgroundColor: _bleuPrincipal,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon:    const Icon(Icons.refresh),
            tooltip: 'Actualiser',
            onPressed: _charger,
          ),
        ],
      ),
      body: _buildCorps(),
    );
  }

  Widget _buildCorps() {
    // État : chargement
    if (_loading) {
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
                onPressed: _charger,
                icon:  const Icon(Icons.refresh),
                label: const Text('Réessayer'),
              ),
            ],
          ),
        ),
      );
    }

    // État : liste vide
    if (_items.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.check_circle_outline, size: 72, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Aucune notification',
              style: TextStyle(color: Colors.grey[600], fontSize: 16),
            ),
            const SizedBox(height: 8),
            Text(
              'Tout est à jour !',
              style: TextStyle(color: Colors.grey[400], fontSize: 13),
            ),
          ],
        ),
      );
    }

    // État : liste des notifications avec pull-to-refresh
    return RefreshIndicator(
      onRefresh: _charger,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: _items.length,
        separatorBuilder: (context, index) => const Divider(height: 1, indent: 64),
        itemBuilder: (context, i) => _CarteNotification(
          item:       _items[i],
          icone:      _icone(_items[i].type),
          couleur:    _couleur(_items[i].severite),
          formatDate: _formatDate,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : carte d'une notification individuelle
// ---------------------------------------------------------------------------

class _CarteNotification extends StatelessWidget {
  final NotificationItem           item;
  final IconData                   icone;
  final Color                      couleur;
  final String Function(DateTime)  formatDate;

  const _CarteNotification({
    required this.item,
    required this.icone,
    required this.couleur,
    required this.formatDate,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      leading: CircleAvatar(
        backgroundColor: couleur.withAlpha(30),
        child: Icon(icone, color: couleur, size: 22),
      ),
      title: Text(
        item.titre,
        style: TextStyle(
          fontWeight: FontWeight.w600,
          fontSize:   14,
          color:      couleur == Colors.orange.shade700
              ? Colors.orange.shade800
              : Colors.black87,
        ),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 2),
          Text(item.message, style: const TextStyle(fontSize: 13)),
          const SizedBox(height: 4),
          Text(
            formatDate(item.date),
            style: TextStyle(fontSize: 11, color: Colors.grey[500]),
          ),
        ],
      ),
      isThreeLine: true,
    );
  }
}
