import { Component } from '@angular/core';

@Component({
    selector: 'app-algorithms',
    imports: [],
    templateUrl: './algorithms.html',
    styleUrl: './algorithms.scss',
})
export class Algorithms {
    scrollTo(sectionId: string): void {
        document
            .getElementById(sectionId)
            ?.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });
    }
}