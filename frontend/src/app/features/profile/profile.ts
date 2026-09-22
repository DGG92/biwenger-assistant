import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
    AuthService,
    BiwengerCredentialStatus,
} from '../../core/services/auth';

@Component({
    selector: 'app-profile',
    imports: [FormsModule],
    templateUrl: './profile.html',
    styleUrl: './profile.scss',
})
export class Profile implements OnInit {

    private readonly authService = inject(AuthService);

    readonly saving = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');

    readonly credentialLoading = signal(true);
    readonly credentialSaving = signal(false);
    readonly credentialStatus = signal<BiwengerCredentialStatus | null>(null);
    readonly credentialErrorMessage = signal('');
    readonly credentialSuccessMessage = signal('');

    newPassword = '';
    repeatedPassword = '';
    showPassword = false;

    biwengerToken = '';
    showBiwengerToken = false;

    ngOnInit(): void {
        this.loadBiwengerCredentialStatus();
    }

    changePassword(): void {
        if (!this.newPassword || !this.repeatedPassword) {
            this.errorMessage.set(
                'Completa los dos campos de contraseña.'
            );
            this.successMessage.set('');
            return;
        }

        if (this.newPassword.length < 8) {
            this.errorMessage.set(
                'La contraseña debe tener al menos 8 caracteres.'
            );
            this.successMessage.set('');
            return;
        }

        if (this.newPassword !== this.repeatedPassword) {
            this.errorMessage.set(
                'Las contraseñas no coinciden.'
            );
            this.successMessage.set('');
            return;
        }

        this.saving.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.changePassword({
            newPassword: this.newPassword,
            repeatedPassword: this.repeatedPassword,
        }).subscribe({
            next: () => {
                this.newPassword = '';
                this.repeatedPassword = '';
                this.showPassword = false;
                this.saving.set(false);

                this.successMessage.set(
                    'Contraseña actualizada correctamente.'
                );
            },
            error: (error) => {
                this.newPassword = '';
                this.repeatedPassword = '';
                this.showPassword = false;
                this.saving.set(false);

                if (error.status === 400) {
                    this.errorMessage.set(
                        'Los datos introducidos no son válidos.'
                    );
                    return;
                }

                this.errorMessage.set(
                    'No se ha podido actualizar la contraseña. Inténtalo de nuevo.'
                );
            },
        });
    }

    saveBiwengerCredential(): void {
        const token = this.biwengerToken.trim();

        if (!token) {
            this.credentialErrorMessage.set(
                'Introduce tu token de Biwenger.'
            );
            this.credentialSuccessMessage.set('');
            return;
        }

        this.credentialSaving.set(true);
        this.credentialErrorMessage.set('');
        this.credentialSuccessMessage.set('');

        this.authService.saveBiwengerCredential({
            token,
        }).subscribe({
            next: (status) => {
                this.credentialStatus.set(status);
                this.biwengerToken = '';
                this.showBiwengerToken = false;
                this.credentialSaving.set(false);

                this.credentialSuccessMessage.set(
                    'Cuenta de Biwenger vinculada correctamente.'
                );
            },
            error: (error) => {
                this.biwengerToken = '';
                this.showBiwengerToken = false;
                this.credentialSaving.set(false);

                if (error.status === 400 || error.status === 401) {
                    this.credentialErrorMessage.set(
                        'El token no es válido para tu usuario de Biwenger.'
                    );
                    return;
                }

                this.credentialErrorMessage.set(
                    'No se ha podido vincular la cuenta de Biwenger. Inténtalo de nuevo.'
                );
            },
        });
    }

    private loadBiwengerCredentialStatus(): void {
        this.credentialLoading.set(true);
        this.credentialErrorMessage.set('');

        this.authService.getBiwengerCredentialStatus()
            .subscribe({
                next: (status) => {
                    this.credentialStatus.set(status);
                    this.credentialLoading.set(false);
                },
                error: () => {
                    this.credentialLoading.set(false);
                    this.credentialErrorMessage.set(
                        'No se ha podido comprobar la vinculación con Biwenger.'
                    );
                },
            });
    }
}