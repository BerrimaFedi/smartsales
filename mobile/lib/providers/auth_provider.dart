import 'package:flutter/material.dart';
import '../models/auth.dart';
import '../services/auth_service.dart';

/// État global d'authentification partagé dans toute l'application via Provider.
class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();

  bool    _isLoggedIn = false;
  String? _username;
  String? _role;
  bool    _isLoading  = false;

  bool    get isLoggedIn => _isLoggedIn;
  String? get username   => _username;
  String? get role       => _role;
  bool    get isLoading  => _isLoading;

  /// Appelé au démarrage de l'app pour restaurer la session existante.
  Future<void> checkAuthStatus() async {
    if (await _authService.isLoggedIn()) {
      _username    = await _authService.getUsername();
      _role        = await _authService.getRole();
      _isLoggedIn  = true;
      notifyListeners();
    }
  }

  /// Tente une connexion.
  /// Retourne null si succès, ou un message d'erreur lisible.
  Future<String?> login(String username, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final AuthResponse auth = await _authService.login(username, password);
      _username   = auth.username;
      _role       = auth.role;
      _isLoggedIn = true;
      return null; // succès
    } on Exception catch (e) {
      return e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Déconnecte l'utilisateur et efface la session persistée.
  Future<void> logout() async {
    await _authService.logout();
    _isLoggedIn = false;
    _username   = null;
    _role       = null;
    notifyListeners();
  }
}
