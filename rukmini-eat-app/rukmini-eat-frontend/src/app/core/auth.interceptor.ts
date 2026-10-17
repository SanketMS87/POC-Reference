import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isApi = req.url.startsWith(environment.apiUrl);
  const isAuthCall = req.url.endsWith('/auth/login') || req.url.endsWith('/auth/register');
  const token = auth.token();

  const outgoing = isApi && !isAuthCall && token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(outgoing).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && isApi && !isAuthCall) {
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};
