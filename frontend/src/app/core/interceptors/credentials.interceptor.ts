import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { CsrfTokenService } from '../services/csrf-token';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {

    const csrfTokenService = inject(CsrfTokenService);
    const csrfToken = csrfTokenService.token();

    const isMutatingRequest =
        req.method !== 'GET' &&
        req.method !== 'HEAD' &&
        req.method !== 'OPTIONS';

    return next(
        req.clone({
            withCredentials: true,
            setHeaders:
                isMutatingRequest && csrfToken
                    ? { 'X-XSRF-TOKEN': csrfToken }
                    : {},
        })
    );
};