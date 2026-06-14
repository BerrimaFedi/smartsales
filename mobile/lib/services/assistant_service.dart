import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/assistant.dart';
import 'auth_service.dart';

/// Gère les appels à l'endpoint assistant intelligent du backend SmartSales.
class AssistantService {
  final AuthService _authService = AuthService();

  /// Construit les headers HTTP avec le JWT récupéré depuis shared_preferences.
  Future<Map<String, String>> _headers() async {
    final token = await _authService.getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    };
  }

  /// Envoie un message à l'assistant et retourne sa réponse.
  ///
  /// Lance une [Exception] avec un message en français si :
  ///   - le serveur est injoignable (erreur réseau)
  ///   - le JWT est expiré / invalide (401 ou 403)
  ///   - le serveur retourne une erreur inattendue (4xx / 5xx)
  Future<AssistantResponse> envoyer(String message) async {
    late http.Response response;
    try {
      response = await http.post(
        Uri.parse('$baseUrl/api/assistant'),
        headers: await _headers(),
        body: jsonEncode({'message': message}),
      );
    } catch (_) {
      throw Exception('Serveur injoignable. Vérifiez votre connexion.');
    }

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return AssistantResponse.fromJson(data);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Session expirée. Veuillez vous reconnecter.');
    } else {
      throw Exception('Erreur de l\'assistant (${response.statusCode}).');
    }
  }
}
