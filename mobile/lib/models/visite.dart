import 'package:flutter/material.dart';

// ---------------------------------------------------------------------------
// Client
// ---------------------------------------------------------------------------

/// Représente un client associé à une visite commerciale.
class Client {
  final int     id;
  final String  nom;
  final String  adresse;
  final double? latitude;
  final double? longitude;

  const Client({
    required this.id,
    required this.nom,
    required this.adresse,
    this.latitude,
    this.longitude,
  });

  factory Client.fromJson(Map<String, dynamic> json) {
    return Client(
      id:        (json['id'] as int?)      ?? 0,
      nom:       (json['nom'] as String?)  ?? 'Client inconnu',
      adresse:   (json['adresse'] as String?) ?? '',
      latitude:  (json['latitude']  as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
    );
  }
}

// ---------------------------------------------------------------------------
// Enum TypeVisite
// ---------------------------------------------------------------------------

/// Type de visite commerciale.
/// Les valeurs lowerCamelCase correspondent aux constantes SCREAMING_SNAKE_CASE de l'API
/// via le getter [apiValue].
enum TypeVisite { prospection, relance, negociation }

extension TypeVisiteExt on TypeVisite {
  /// Chaîne envoyée/reçue par l'API REST.
  String get apiValue {
    switch (this) {
      case TypeVisite.prospection: return 'PROSPECTION';
      case TypeVisite.relance:     return 'RELANCE';
      case TypeVisite.negociation: return 'NEGOCIATION';
    }
  }

  /// Libellé lisible en français.
  String get libelle {
    switch (this) {
      case TypeVisite.prospection: return 'Prospection';
      case TypeVisite.relance:     return 'Relance';
      case TypeVisite.negociation: return 'Négociation';
    }
  }

  /// Convertit la chaîne renvoyée par l'API en valeur d'enum.
  static TypeVisite fromString(String s) => TypeVisite.values.firstWhere(
    (e) => e.apiValue == s,
    orElse: () => TypeVisite.prospection,
  );
}

// ---------------------------------------------------------------------------
// Enum StatutVisite
// ---------------------------------------------------------------------------

/// Statut d'avancement d'une visite.
/// Les valeurs lowerCamelCase correspondent aux constantes SCREAMING_SNAKE_CASE de l'API
/// via le getter [apiValue].
enum StatutVisite { planifiee, enCours, terminee, annulee }

extension StatutVisiteExt on StatutVisite {
  /// Chaîne envoyée/reçue par l'API REST.
  String get apiValue {
    switch (this) {
      case StatutVisite.planifiee: return 'PLANIFIEE';
      case StatutVisite.enCours:   return 'EN_COURS';
      case StatutVisite.terminee:  return 'TERMINEE';
      case StatutVisite.annulee:   return 'ANNULEE';
    }
  }

  /// Libellé lisible en français.
  String get libelle {
    switch (this) {
      case StatutVisite.planifiee: return 'Planifiée';
      case StatutVisite.enCours:   return 'En cours';
      case StatutVisite.terminee:  return 'Terminée';
      case StatutVisite.annulee:   return 'Annulée';
    }
  }

  /// Couleur associée au statut pour les badges.
  Color get couleur {
    switch (this) {
      case StatutVisite.planifiee: return Colors.blue;
      case StatutVisite.enCours:   return Colors.orange;
      case StatutVisite.terminee:  return Colors.green;
      case StatutVisite.annulee:   return Colors.grey;
    }
  }

  /// Convertit la chaîne renvoyée par l'API en valeur d'enum.
  static StatutVisite fromString(String s) => StatutVisite.values.firstWhere(
    (e) => e.apiValue == s,
    orElse: () => StatutVisite.planifiee,
  );
}

// ---------------------------------------------------------------------------
// Visite
// ---------------------------------------------------------------------------

/// Représente une visite commerciale telle que renvoyée par l'API.
/// [client] est nullable : certaines visites peuvent ne pas avoir de client rattaché.
class Visite {
  final int          id;
  final Client?      client;         // nullable — peut être absent dans la réponse JSON
  final TypeVisite   type;
  final StatutVisite statut;
  final DateTime     dateVisite;
  final String?      compteRendu;
  final double?      montant;
  final int?         ordreTournee;
  // Horodatages de présence (null si non encore enregistrés)
  final DateTime?    checkIn;
  final DateTime?    checkOut;
  // Coordonnées GPS au moment du check-in / check-out (optionnelles)
  final double?      checkInLatitude;
  final double?      checkInLongitude;
  final double?      checkOutLatitude;
  final double?      checkOutLongitude;

  const Visite({
    required this.id,
    this.client,
    required this.type,
    required this.statut,
    required this.dateVisite,
    this.compteRendu,
    this.montant,
    this.ordreTournee,
    this.checkIn,
    this.checkOut,
    this.checkInLatitude,
    this.checkInLongitude,
    this.checkOutLatitude,
    this.checkOutLongitude,
  });

  factory Visite.fromJson(Map<String, dynamic> json) {
    // Parsing défensif : aucun champ ne provoque de crash si null/absent.
    final clientJson = json['client'];

    return Visite(
      id:     (json['id'] as int?) ?? 0,
      // Cast sûr : on vérifie la non-nullité avant de déléguer à Client.fromJson
      client: clientJson != null
          ? Client.fromJson(clientJson as Map<String, dynamic>)
          : null,
      // fromString a déjà un fallback ; on sécurise le cast String en amont
      type:              TypeVisiteExt.fromString((json['type']    as String?) ?? ''),
      statut:            StatutVisiteExt.fromString((json['statut'] as String?) ?? ''),
      // DateTime.tryParse retourne null si la chaîne est absente ou malformée
      dateVisite:        DateTime.tryParse((json['dateVisite'] as String?) ?? '') ?? DateTime.now(),
      compteRendu:       json['compteRendu'] as String?,
      montant:           (json['montant'] as num?)?.toDouble(),
      ordreTournee:      json['ordreTournee'] as int?,
      // Champs de présence : null si absents du JSON
      checkIn:           json['checkIn']  != null ? DateTime.tryParse(json['checkIn']  as String) : null,
      checkOut:          json['checkOut'] != null ? DateTime.tryParse(json['checkOut'] as String) : null,
      checkInLatitude:   (json['checkInLatitude']   as num?)?.toDouble(),
      checkInLongitude:  (json['checkInLongitude']  as num?)?.toDouble(),
      checkOutLatitude:  (json['checkOutLatitude']  as num?)?.toDouble(),
      checkOutLongitude: (json['checkOutLongitude'] as num?)?.toDouble(),
    );
  }
}
