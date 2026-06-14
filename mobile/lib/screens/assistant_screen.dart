import 'package:flutter/material.dart';
import '../models/assistant.dart';
import '../services/assistant_service.dart';

/// Couleur principale de l'app (thème bleu SmartSales).
const Color _bleuPrincipal = Color(0xFF1565C0);

/// Écran de chat avec l'assistant intelligent SmartSales.
///
/// - Messages utilisateur à droite (bulle bleue).
/// - Messages assistant à gauche (bulle grise).
/// - Suggestions cliquables sous chaque réponse de l'assistant.
/// - Indicateur de chargement pendant l'attente de la réponse.
class AssistantScreen extends StatefulWidget {
  const AssistantScreen({super.key});

  @override
  State<AssistantScreen> createState() => _AssistantScreenState();
}

class _AssistantScreenState extends State<AssistantScreen> {
  final AssistantService      _service       = AssistantService();
  final TextEditingController _champTexte    = TextEditingController();
  final ScrollController      _scrollCtrl    = ScrollController();

  /// Historique de la conversation (messages + suggestions affichées séparément).
  final List<_EntreeConversation> _entrees = [];

  bool _enAttente = false;

  // Suggestions affichées au démarrage pour guider le commercial.
  static const List<String> _suggestionsInitiales = [
    'Mon CA ce mois',
    'Quels clients relancer ?',
    'Mes visites de la semaine',
    'Aide',
  ];

  @override
  void initState() {
    super.initState();
    // Message de bienvenue affiché immédiatement sans appel réseau.
    _entrees.add(_EntreeConversation(
      message: MessageChat(
        texte:         'Bonjour ! Je suis votre assistant SmartSales. '
                       'Comment puis-je vous aider ?',
        estUtilisateur: false,
        horodatage:    DateTime.now(),
      ),
      suggestions: _suggestionsInitiales,
    ));
  }

  @override
  void dispose() {
    _champTexte.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Envoi d'un message
  // ---------------------------------------------------------------------------

  /// Envoie le texte saisi (ou une suggestion cliquée) à l'assistant.
  Future<void> _envoyerMessage(String texte) async {
    final contenu = texte.trim();
    if (contenu.isEmpty || _enAttente) return;

    _champTexte.clear();

    setState(() {
      _enAttente = true;
      // Ajoute le message de l'utilisateur sans suggestions.
      _entrees.add(_EntreeConversation(
        message: MessageChat(
          texte:         contenu,
          estUtilisateur: true,
          horodatage:    DateTime.now(),
        ),
        suggestions: const [],
      ));
    });

    _defilerVersLeBas();

    try {
      final reponse = await _service.envoyer(contenu);
      if (mounted) {
        setState(() {
          _entrees.add(_EntreeConversation(
            message: MessageChat(
              texte:         reponse.reply,
              estUtilisateur: false,
              horodatage:    DateTime.now(),
            ),
            suggestions: reponse.suggestions,
          ));
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        // Affiche l'erreur sous forme de message assistant (non intrusif).
        setState(() {
          _entrees.add(_EntreeConversation(
            message: MessageChat(
              texte:         e.toString().replaceFirst('Exception: ', ''),
              estUtilisateur: false,
              horodatage:    DateTime.now(),
            ),
            suggestions: const [],
            estErreur: true,
          ));
        });
      }
    } finally {
      if (mounted) {
        setState(() => _enAttente = false);
        _defilerVersLeBas();
      }
    }
  }

  /// Fait défiler la liste vers le dernier message après le rendu.
  void _defilerVersLeBas() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) {
        _scrollCtrl.animateTo(
          _scrollCtrl.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.smart_toy_outlined, size: 22),
            SizedBox(width: 8),
            Text('Assistant SmartSales'),
          ],
        ),
        backgroundColor: _bleuPrincipal,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          // Liste de messages
          Expanded(
            child: ListView.builder(
              controller: _scrollCtrl,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              itemCount: _entrees.length + (_enAttente ? 1 : 0),
              itemBuilder: (context, index) {
                // Dernière entrée = indicateur de frappe si en attente
                if (_enAttente && index == _entrees.length) {
                  return const _IndicateurChargement();
                }
                final entree = _entrees[index];
                return _BulleMessage(
                  entree:    entree,
                  onSuggestion: _envoyerMessage,
                );
              },
            ),
          ),

          // Barre de saisie
          _BarreSaisie(
            controller: _champTexte,
            enAttente:  _enAttente,
            onEnvoyer:  _envoyerMessage,
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Modèle interne : une entrée = message + ses suggestions éventuelles
// ---------------------------------------------------------------------------

class _EntreeConversation {
  final MessageChat   message;
  final List<String>  suggestions;
  final bool          estErreur;

  const _EntreeConversation({
    required this.message,
    required this.suggestions,
    this.estErreur = false,
  });
}

// ---------------------------------------------------------------------------
// Widget : bulle de message + suggestions
// ---------------------------------------------------------------------------

class _BulleMessage extends StatelessWidget {
  final _EntreeConversation     entree;
  final void Function(String)   onSuggestion;

  const _BulleMessage({required this.entree, required this.onSuggestion});

  @override
  Widget build(BuildContext context) {
    final estUtilisateur = entree.message.estUtilisateur;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment:
            estUtilisateur ? CrossAxisAlignment.end : CrossAxisAlignment.start,
        children: [
          // Bulle principale
          Row(
            mainAxisAlignment:
                estUtilisateur ? MainAxisAlignment.end : MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              // Avatar de l'assistant
              if (!estUtilisateur) ...[
                CircleAvatar(
                  radius: 14,
                  backgroundColor: _bleuPrincipal,
                  child: const Icon(Icons.smart_toy_outlined,
                      size: 16, color: Colors.white),
                ),
                const SizedBox(width: 6),
              ],

              // Bulle de texte (contrainte en largeur)
              Flexible(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: estUtilisateur
                        ? _bleuPrincipal
                        : entree.estErreur
                            ? Colors.red[50]
                            : Colors.grey[200],
                    borderRadius: BorderRadius.only(
                      topLeft:     const Radius.circular(16),
                      topRight:    const Radius.circular(16),
                      bottomLeft:  Radius.circular(estUtilisateur ? 16 : 4),
                      bottomRight: Radius.circular(estUtilisateur ? 4  : 16),
                    ),
                  ),
                  child: Text(
                    entree.message.texte,
                    style: TextStyle(
                      color: estUtilisateur
                          ? Colors.white
                          : entree.estErreur
                              ? Colors.red[800]
                              : Colors.black87,
                      fontSize: 15,
                    ),
                  ),
                ),
              ),

              // Espace à droite pour les messages assistant
              if (estUtilisateur) ...[
                const SizedBox(width: 6),
                CircleAvatar(
                  radius: 14,
                  backgroundColor: Colors.blue[100],
                  child: const Icon(Icons.person_outline,
                      size: 16, color: _bleuPrincipal),
                ),
              ],
            ],
          ),

          // Puces de suggestions cliquables (uniquement pour les messages assistant)
          if (entree.suggestions.isNotEmpty) ...[
            const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.only(left: 36),
              child: Wrap(
                spacing: 6,
                runSpacing: 6,
                children: entree.suggestions
                    .map((s) => _PuceSuggestion(texte: s, onTap: onSuggestion))
                    .toList(),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : puce suggestion cliquable
// ---------------------------------------------------------------------------

class _PuceSuggestion extends StatelessWidget {
  final String                  texte;
  final void Function(String)   onTap;

  const _PuceSuggestion({required this.texte, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onTap(texte),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color:        Colors.white,
          border:       Border.all(color: _bleuPrincipal.withAlpha(180)),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          texte,
          style: const TextStyle(
            color:     _bleuPrincipal,
            fontSize:  13,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : indicateur de frappe de l'assistant (3 points animés)
// ---------------------------------------------------------------------------

class _IndicateurChargement extends StatelessWidget {
  const _IndicateurChargement();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          CircleAvatar(
            radius: 14,
            backgroundColor: _bleuPrincipal,
            child: const Icon(Icons.smart_toy_outlined,
                size: 16, color: Colors.white),
          ),
          const SizedBox(width: 6),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color:        Colors.grey[200],
              borderRadius: const BorderRadius.only(
                topLeft:     Radius.circular(16),
                topRight:    Radius.circular(16),
                bottomRight: Radius.circular(16),
                bottomLeft:  Radius.circular(4),
              ),
            ),
            child: const SizedBox(
              width:  24,
              height: 14,
              child:  _PointsAnimes(),
            ),
          ),
        ],
      ),
    );
  }
}

/// Trois points clignotants simulant la frappe de l'assistant.
class _PointsAnimes extends StatefulWidget {
  const _PointsAnimes();

  @override
  State<_PointsAnimes> createState() => _PointsAnimesState();
}

class _PointsAnimesState extends State<_PointsAnimes>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync:    this,
      duration: const Duration(milliseconds: 900),
    )..repeat();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, child) {
        // Chaque point s'allume à tour de rôle (0-33%, 33-66%, 66-100%)
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: List.generate(3, (i) {
            final actif = (_ctrl.value * 3).floor() == i;
            return Container(
              width:  6,
              height: 6,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: actif ? Colors.grey[600] : Colors.grey[400],
              ),
            );
          }),
        );
      },
    );
  }
}

// ---------------------------------------------------------------------------
// Widget : barre de saisie en bas de l'écran
// ---------------------------------------------------------------------------

class _BarreSaisie extends StatelessWidget {
  final TextEditingController   controller;
  final bool                    enAttente;
  final void Function(String)   onEnvoyer;

  const _BarreSaisie({
    required this.controller,
    required this.enAttente,
    required this.onEnvoyer,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        padding:    const EdgeInsets.fromLTRB(12, 8, 8, 8),
        decoration: BoxDecoration(
          color:  Colors.white,
          border: Border(top: BorderSide(color: Colors.grey[300]!)),
        ),
        child: Row(
          children: [
            // Champ de saisie
            Expanded(
              child: TextField(
                controller:   controller,
                enabled:      !enAttente,
                maxLines:     null,  // multi-lignes si besoin
                textInputAction: TextInputAction.send,
                onSubmitted:  enAttente ? null : onEnvoyer,
                decoration: InputDecoration(
                  hintText:        'Posez votre question…',
                  hintStyle:       TextStyle(color: Colors.grey[400]),
                  filled:          true,
                  fillColor:       Colors.grey[100],
                  contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 10),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide:  BorderSide.none,
                  ),
                ),
              ),
            ),

            const SizedBox(width: 8),

            // Bouton d'envoi
            Material(
              color:        enAttente ? Colors.grey[300] : _bleuPrincipal,
              borderRadius: BorderRadius.circular(24),
              child: InkWell(
                borderRadius: BorderRadius.circular(24),
                onTap: enAttente
                    ? null
                    : () => onEnvoyer(controller.text),
                child: const Padding(
                  padding: EdgeInsets.all(12),
                  child:   Icon(Icons.send, color: Colors.white, size: 20),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
