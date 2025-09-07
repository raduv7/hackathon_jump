import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../security.config';

export interface Automation {
  id: number;
  title: string;
  automationType: string;
  mediaPlatform: string;
  description: string;
  example?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AutomationsService {
  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllAutomations(): Observable<Automation[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Automation[]>(`${API_BASE_URL}/automations`, { headers });
  }

  getAutomationById(id: number): Observable<Automation> {
    const headers = this.getAuthHeaders();
    return this.http.get<Automation>(`${API_BASE_URL}/automations/${id}`, { headers });
  }

  createAutomation(title: string, automationType: string, mediaPlatform: string, description: string, example?: string): Observable<Automation> {
    const headers = this.getAuthHeaders();
    const params = new URLSearchParams();
    params.set('title', title);
    params.set('automationType', automationType);
    params.set('mediaPlatform', mediaPlatform);
    params.set('description', description);
    if (example) {
      params.set('example', example);
    }
    
    return this.http.post<Automation>(`${API_BASE_URL}/automations?${params.toString()}`, {}, { headers });
  }

  updateAutomation(id: number, title: string, automationType: string, mediaPlatform: string, description: string, example?: string): Observable<Automation> {
    const headers = this.getAuthHeaders();
    const params = new URLSearchParams();
    params.set('title', title);
    params.set('automationType', automationType);
    params.set('mediaPlatform', mediaPlatform);
    params.set('description', description);
    if (example) {
      params.set('example', example);
    }
    
    return this.http.put<Automation>(`${API_BASE_URL}/automations/${id}?${params.toString()}`, {}, { headers });
  }

  deleteAutomation(id: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete<string>(`${API_BASE_URL}/automations/${id}`, { headers });
  }

  subscribeToAutomation(id: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post<string>(`${API_BASE_URL}/automations/${id}/subscribe`, {}, { headers });
  }

  unsubscribeFromAutomation(id: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete<string>(`${API_BASE_URL}/automations/${id}/subscribe`, { headers });
  }
}
