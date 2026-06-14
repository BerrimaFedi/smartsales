/// URL de base de l'API SmartSales.
///
/// Adapter selon l'environnement :
/// - Navigateur web / iOS Simulator : http://localhost:8080  ✓
/// - Émulateur Android              : http://10.0.2.2:8080
/// - Téléphone physique             : `http://192.168.1.x:8080`  (IP du PC sur le réseau local)
const String baseUrl = 'http://localhost:8080';
