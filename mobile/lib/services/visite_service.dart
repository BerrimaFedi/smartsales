import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/visite.dart';
import 'auth_service.dart';

/// Gère les appels API pour les visites du commercial connecté.
class VisiteService {
  final AuthService _authService = AuthService();

  /// Construit les headers HTTP avec le JWT récupéré depuis shared_preferences.
  Future<Map<String, String>> _headers() async {
    final token = await _authService.getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    };
  }

  /// Récupère toutes les visites du commercial connecté (filtrées par le backend via JWT).
  Future<List<Visite>> getMesVisites() async {
    late http.Response response;
    try {
      response = await http.get(
        Uri.parse('$baseUrl/api/visites'),
        headers: await _headers(),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body) as List<dynamic>;
      return data.map((e) => Visite.fromJson(e as Map<String, dynamic>)).toList();
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée. Veuillez vous reconnecter.');
    } else {
      throw Exception('Erreur serveur (${response.statusCode}).');
    }
  }

  /// Récupère une visite par son identifiant (pour rafraîchir l'écran de détail).
  Future<Visite> getVisiteById(int id) async {
    late http.Response response;
    try {
      response = await http.get(
        Uri.parse('$baseUrl/api/visites/$id'),
        headers: await _headers(),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }
    if (response.statusCode == 200) {
      return Visite.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée. Veuillez vous reconnecter.');
    } else {
      throw Exception('Erreur serveur (${response.statusCode}).');
    }
  }

  /// Enregistre le check-in avec les coordonnées GPS optionnelles.
  /// Retourne la visite mise à jour (statut peut passer à EN_COURS).
  Future<Visite> checkIn(int id, {double? lat, double? lng}) async {
    final body = <String, dynamic>{};
    if (lat != null) body['latitude']  = lat;
    if (lng != null) body['longitude'] = lng;

    late http.Response response;
    try {
      response = await http.post(
        Uri.parse('$baseUrl/api/visites/$id/checkin'),
        headers: await _headers(),
        body: jsonEncode(body),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }
    if (response.statusCode == 200) {
      return Visite.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée ou accès refusé.');
    } else {
      throw Exception('Erreur lors du check-in (${response.statusCode}).');
    }
  }

  /// Enregistre le check-out avec les coordonnées GPS optionnelles.
  Future<Visite> checkOut(int id, {double? lat, double? lng}) async {
    final body = <String, dynamic>{};
    if (lat != null) body['latitude']  = lat;
    if (lng != null) body['longitude'] = lng;

    late http.Response response;
    try {
      response = await http.post(
        Uri.parse('$baseUrl/api/visites/$id/checkout'),
        headers: await _headers(),
        body: jsonEncode(body),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }
    if (response.statusCode == 200) {
      return Visite.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée ou accès refusé.');
    } else {
      throw Exception('Erreur lors du check-out (${response.statusCode}).');
    }
  }

  /// Met à jour le statut, le compte-rendu et/ou le montant d'une visite.
  ///
  /// Seuls les champs non-null sont envoyés dans le corps PATCH.
  Future<void> updateVisite(
    int id, {
    required StatutVisite statut,
    String? compteRendu,
    double? montant,
  }) async {
    // Construction du corps : on envoie toujours le statut
    // apiValue renvoie la chaîne SCREAMING_SNAKE_CASE attendue par le backend
    final body = <String, dynamic>{'statut': statut.apiValue};
    if (compteRendu != null) body['compteRendu'] = compteRendu;
    if (montant != null)     body['montant']     = montant;

    late http.Response response;
    try {
      response = await http.patch(
        Uri.parse('$baseUrl/api/visites/$id'),
        headers: await _headers(),
        body: jsonEncode(body),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }

    if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée. Veuillez vous reconnecter.');
    } else if (response.statusCode >= 400) {
      throw Exception('Erreur lors de la mise à jour (${response.statusCode}).');
    }
  }
}
