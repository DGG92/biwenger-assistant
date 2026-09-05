import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
    AdminUsersService,
    AvailableManager,
} from '../../core/services/admin-users';

@Component({
    selector: 'app-admin-users',
    imports: [FormsModule],
    templateUrl: './admin-users.html',
    styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit {

    private readonly adminUsersService = inject(AdminUsersService);

    readonly managers = signal<AvailableManager[]>([]);
    readonly loading = signal(true);
    readonly creating = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');

    username = '';
    password = '';
    managerId: number | null = null;
    repeatPassword = '';
    showPassword = false;

    ngOnInit(): void {
        this.loadManagers();
    }

    createUser(): void {
        if (
            !this.username.trim()
            || !this.password
            || !this.repeatPassword
            || this.managerId === null
        ) {
            this.errorMessage.set(
                'Completa todos los campos antes de crear el usuario.'
            );
            return;
        }

        if (this.password !== this.repeatPassword) {
            this.errorMessage.set(
                'Las contraseñas no coinciden.'
            );

            this.password = '';
            this.repeatPassword = '';

            return;
        }

        this.creating.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.adminUsersService.createUser({
            username: this.username.trim(),
            password: this.password,
            managerId: this.managerId,
        }).subscribe({
            next: () => {
                this.successMessage.set(
                    `Usuario "${this.username.trim()}" creado correctamente.`
                );

                this.username = '';
                this.password = '';
                this.repeatPassword = '';
                this.managerId = null;
                this.showPassword = false;
                this.creating.set(false);

                this.loadManagers();
            },
            error: (error) => {
                this.creating.set(false);
                this.password = '';
                this.repeatPassword = '';
                this.showPassword = false;

                if (error.status === 400) {
                    this.errorMessage.set(
                        'Los datos introducidos no son válidos.'
                    );
                    return;
                }

                if (error.status === 409) {
                    this.errorMessage.set(
                        'Ese usuario o manager ya está asignado.'
                    );
                    return;
                }

                this.errorMessage.set(
                    'No se ha podido crear el usuario. Inténtalo de nuevo.'
                );
            },
        });
    }

    private loadManagers(): void {
        this.loading.set(true);
        this.errorMessage.set('');

        this.adminUsersService.getAvailableManagers().subscribe({
            next: (managers) => {
                this.managers.set(managers);
                this.loading.set(false);
            },
            error: () => {
                this.errorMessage.set(
                    'No se han podido cargar los managers disponibles.'
                );
                this.loading.set(false);
            },
        });
    }
}