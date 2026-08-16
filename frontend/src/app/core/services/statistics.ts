import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import {
    StatisticParam,
    StatisticResponse,
} from '../models/statistics.model';

@Injectable({
    providedIn: 'root',
})
export class StatisticsService {
    private readonly http = inject(HttpClient);

    getReport(
        report: string,
        param?: StatisticParam
    ): Observable<StatisticResponse> {
        let params = new HttpParams()
            .set('report', report);

        if (param) {
            params = params.set('param', param);
        }

        return this.http.get<StatisticResponse>(
            `${API_CONFIG.baseUrl}/biwenger/reports`,
            {
                params,
            }
        );
    }
}