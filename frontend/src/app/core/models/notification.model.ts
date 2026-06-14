export interface NotificationItem {
  type: 'RAPPEL' | 'RETARD' | 'SUGGESTION';
  titre: string;
  message: string;
  date: string; // ISO datetime renvoyé par le backend
  severite: 'info' | 'warning';
}

export interface NotificationResponse {
  count: number;
  notifications: NotificationItem[];
}
