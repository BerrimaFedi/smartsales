/// Modèle de réponse de l'assistant intelligent SmartSales.
class AssistantResponse {
  /// Texte de la réponse générée par l'assistant.
  final String reply;

  /// Suggestions de questions de suivi (peut être vide).
  final List<String> suggestions;

  const AssistantResponse({
    required this.reply,
    required this.suggestions,
  });

  /// Désérialise la réponse JSON de l'API.
  factory AssistantResponse.fromJson(Map<String, dynamic> json) {
    final rawSuggestions = json['suggestions'];
    final List<String> suggestions;
    if (rawSuggestions is List) {
      suggestions = rawSuggestions.map((e) => e.toString()).toList();
    } else {
      suggestions = const [];
    }
    return AssistantResponse(
      reply:       (json['reply'] as String?) ?? '',
      suggestions: suggestions,
    );
  }
}

/// Représente un message dans la conversation (utilisateur ou assistant).
class MessageChat {
  final String  texte;
  final bool    estUtilisateur; // true = message de l'utilisateur, false = assistant
  final DateTime horodatage;

  const MessageChat({
    required this.texte,
    required this.estUtilisateur,
    required this.horodatage,
  });
}
