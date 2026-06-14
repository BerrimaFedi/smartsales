/// Représente une notification individuelle renvoyée par l'API SmartSales.
class NotificationItem {
  /// Type de notification : "RAPPEL", "RETARD" ou "SUGGESTION".
  final String type;

  /// Titre court affiché en gras dans la liste.
  final String titre;

  /// Description détaillée de la notification.
  final String message;

  /// Date de référence (visite concernée ou instant de calcul).
  final DateTime date;

  /// Niveau de sévérité : "info" (rappels, suggestions) ou "warning" (retards).
  final String severite;

  const NotificationItem({
    required this.type,
    required this.titre,
    required this.message,
    required this.date,
    required this.severite,
  });

  /// Désérialise un objet JSON en [NotificationItem] (parsing défensif).
  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    return NotificationItem(
      type:     (json['type']     as String?) ?? 'INFO',
      titre:    (json['titre']    as String?) ?? '',
      message:  (json['message']  as String?) ?? '',
      date:     DateTime.tryParse((json['date'] as String?) ?? '') ?? DateTime.now(),
      severite: (json['severite'] as String?) ?? 'info',
    );
  }
}

/// Réponse complète de GET /api/notifications.
class NotificationResponse {
  /// Nombre total de notifications (utilisé pour le badge).
  final int count;

  /// Liste triée (retards en premier, puis par date).
  final List<NotificationItem> notifications;

  const NotificationResponse({
    required this.count,
    required this.notifications,
  });

  /// Désérialise la réponse JSON de l'API.
  factory NotificationResponse.fromJson(Map<String, dynamic> json) {
    final rawList = json['notifications'];
    final List<NotificationItem> items;
    if (rawList is List) {
      items = rawList
          .whereType<Map<String, dynamic>>()
          .map(NotificationItem.fromJson)
          .toList();
    } else {
      items = const [];
    }
    return NotificationResponse(
      count:         (json['count'] as int?) ?? 0,
      notifications: items,
    );
  }
}
