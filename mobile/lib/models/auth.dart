/// Réponse renvoyée par l'API lors d'une connexion réussie.
class AuthResponse {
  final String token;
  final String username;
  final String role;

  const AuthResponse({
    required this.token,
    required this.username,
    required this.role,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'] as String,
      username: json['username'] as String,
      role: json['role'] as String,
    );
  }
}
