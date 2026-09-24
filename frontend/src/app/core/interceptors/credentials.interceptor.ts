import { HttpInterceptorFn } from '@angular/common/http';

function getCookie(name: string): string | null {
    const cookie = document.cookie
        .split('; ')
        .find((row) => row.startsWith(`${name}=`));

    if (!cookie) {
        return null;
    }

    return decodeURIComponent(cookie.substring(name.length + 1));
}

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
    const csrfToken = getCookie('XSRF-TOKEN');

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