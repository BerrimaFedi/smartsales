import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewChecked,
  signal,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssistantService } from '../../core/services/assistant.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  suggestions?: string[];
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.scss',
})
export class Chatbot implements AfterViewChecked {
  private readonly assistantSvc = inject(AssistantService);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

  readonly isOpen = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: 'Bonjour ! Je suis votre assistant SmartSales. Posez-moi une question ou cliquez sur une suggestion.',
      suggestions: ['Mes visites aujourd\'hui ?', 'Mon CA ce mois ?', 'Aide — que peux-tu faire ?'],
    },
  ]);

  inputText = '';

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggle(): void {
    this.isOpen.update(v => !v);
    this.error.set(null);
  }

  send(text?: string): void {
    const msg = (text ?? this.inputText).trim();
    if (!msg || this.loading()) return;

    this.messages.update(list => [...list, { role: 'user', text: msg }]);
    this.inputText = '';
    this.loading.set(true);
    this.error.set(null);

    this.assistantSvc.chat(msg).subscribe({
      next: res => {
        this.messages.update(list => [
          ...list,
          { role: 'assistant', text: res.reply, suggestions: res.suggestions },
        ]);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Une erreur est survenue. Veuillez réessayer.');
        this.loading.set(false);
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }
}
