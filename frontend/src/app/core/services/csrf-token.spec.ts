import { TestBed } from '@angular/core/testing';
import { CsrfTokenService } from './csrf-token';

describe('CsrfTokenService', () => {
    let service: CsrfTokenService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CsrfTokenService);
    });

    it('should store the CSRF token', () => {
        service.setToken('csrf-test-token');

        expect(service.token()).toBe('csrf-test-token');
    });

    it('should clear the CSRF token', () => {
        service.setToken('csrf-test-token');

        service.clear();

        expect(service.token()).toBeNull();
    });
});
