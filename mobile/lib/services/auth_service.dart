import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';
import '../models/auth.dart';

/// Gère les appels d'authentification et la persistance locale du JWT.
/// Utilise shared_preferences (compatible web, Android et iOS).
class AuthService {
  static const _keyToken    = 'jwt_token';
  static const _keyUsername = 'username';
  static const _keyRole     = 'role';

  /// Envoie les identifiants au backend et stocke le token en cas de succès.
  /// Lève une [Exception] avec un message lisible si la connexion échoue.
  Future<AuthResponse> login(String username, String password) async {
    late http.Response response;

    // Séparation nette : erreur réseau vs erreur applicative
    try {
      response = await http.post(
        Uri.parse('$baseUrl/api/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'username': username, 'password': password}),
      );
    } catch (_) {
      throw Exception(
        'Serveur injoignable. Vérifiez votre connexion et que le backend est démarré.',
      );
    }

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      final auth = AuthResponse.fromJson(data);

      // Persistance dans shared_preferences (localStorage sur web, fichier sur mobile)
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_keyToken,    auth.token);
      await prefs.setString(_keyUsername, auth.username);
      await prefs.setString(_keyRole,     auth.role);

      return auth;
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception(
        "Identifiants invalides. Vérifiez votre nom d'utilisateur et mot de passe.",
      );
    } else {
      throw Exception('Erreur serveur (${response.statusCode}). Réessayez plus tard.');
    }
  }

  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_keyToken);
  }

  Future<String?> getUsername() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_keyUsername);
  }

  Future<String?> getRole() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_keyRole);
  }

  /// Retourne true si un token est présent en stockage local.
  Future<bool> isLoggedIn() async {
    final token = await getToken();
    return token != null && token.isNotEmpty;
  }

  /// Supprime toutes les données de session (déconnexion).
  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyToken);
    await prefs.remove(_keyUsername);
    await prefs.remove(_keyRole);
  }
}
