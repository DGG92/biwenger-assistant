import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';

  readonly loading = signal(false);
  readonly errorMessage = signal('');

  submit(): void {

    if (!this.username.trim() || !this.password) {
      this.errorMessage.set(
        'Introduce usuario y contraseña.'
      );
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login({
      username: this.username.trim(),
      password: this.password,
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/dashboard');
      },
      error: (error: unknown) => {
        this.loading.set(false);

        if (
          error instanceof HttpErrorResponse
          && error.status === 401
        ) {
          this.errorMessage.set(
            'Usuario o contraseña incorrectos.'
          );
          return;
        }

        if (error instanceof TimeoutError) {
          this.errorMessage.set(
            'El servidor está tardando demasiado en responder.'
          );
          return;
        }

        this.errorMessage.set(
          'No se ha podido iniciar sesión.'
        );
      },
    });
  }
}