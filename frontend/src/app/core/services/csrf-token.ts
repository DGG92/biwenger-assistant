import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class CsrfTokenService {

    private readonly tokenSignal = signal<string | null>(null);

    readonly token = this.tokenSignal.asReadonly();

    setToken(token: string): void {
        this.tokenSignal.set(token);
    }

    clear(): void {
        this.tokenSignal.set(null);
    }
}