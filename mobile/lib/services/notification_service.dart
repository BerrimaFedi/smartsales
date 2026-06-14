import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/notification.dart';
import 'auth_service.dart';

/// Gère les appels à l'endpoint notifications du backend SmartSales.
class NotificationService {
  final AuthService _authService = AuthService();

  /// Construit les headers HTTP avec le JWT récupéré depuis shared_preferences.
  Future<Map<String, String>> _headers() async {
    final token = await _authService.getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    };
  }

  /// Récupère les notifications calculées à la volée par le backend.
  ///
  /// Lance une [Exception] avec un message en français si :
  ///   - le serveur est injoignable (erreur réseau)
  ///   - le JWT est expiré / invalide (401 ou 403)
  ///   - le serveur retourne une erreur inattendue
  Future<NotificationResponse> getNotifications() async {
    late http.Response response;
    try {
      response = await http.get(
        Uri.parse('$baseUrl/api/notifications'),
        headers: await _headers(),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return NotificationResponse.fromJson(data);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée. Veuillez vous reconnecter.');
    } else {
      throw Exception('Erreur lors du chargement des notifications (${response.statusCode}).');
    }
  }
}
