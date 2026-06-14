import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'screens/login_screen.dart';
import 'screens/planning_screen.dart';

void main() {
  runApp(
    // Provider injecté à la racine pour être accessible dans tout l'arbre de widgets
    ChangeNotifierProvider(
      create: (_) => AuthProvider(),
      child: const SmartSalesApp(),
    ),
  );
}

class SmartSalesApp extends StatelessWidget {
  const SmartSalesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SmartSales',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        primaryColor: Colors.blue[700],
        useMaterial3: true,
      ),
      home: const _AuthGate(),
    );
  }
}

/// Vérifie au démarrage si un token est stocké et redirige vers le bon écran.
class _AuthGate extends StatefulWidget {
  const _AuthGate();

  @override
  State<_AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<_AuthGate> {
  bool _checking = true;

  @override
  void initState() {
    super.initState();
    _checkAuth();
  }

  Future<void> _checkAuth() async {
    // Restaure la session persistée (token stocké localement)
    await context.read<AuthProvider>().checkAuthStatus();
    if (mounted) setState(() => _checking = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_checking) {
      // Écran de chargement initial (vérification du token)
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    return context.watch<AuthProvider>().isLoggedIn
        ? const PlanningScreen()
        : const LoginScreen();
  }
}
