import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="callback-card">
        <h2>Processing authentication...</h2>
        <div class="spinner"></div>
        <p *ngIf="error" class="error">{{ error }}</p>
        <p *ngIf="success" class="success">{{ success }}</p>
      </div>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px;
    }
    
    .callback-card {
      background: rgba(255,255,255,0.9);
      backdrop-filter: blur(6px);
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0,0,0,0.12);
      padding: 32px 28px;
      max-width: 400px;
      width: 100%;
      text-align: center;
    }
    
    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid #f3f3f3;
      border-top: 4px solid #4CAF50;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 20px auto;
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    .error { color: #f44336; margin-top: 16px; }
    .success { color: #4CAF50; margin-top: 16px; }
  `]
})
export class CallbackComponent implements OnInit {
  error: string | null = null;
  success: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    // Get query parameters from the callback URL
    this.route.queryParams.subscribe(params => {
      const code = params['code'];
      const error = params['error'];
      
      if (error) {
        this.error = `Authentication failed: ${error}`;
        setTimeout(() => this.router.navigate(['/login']), 3000);
      } else if (code) {
        this.success = 'Authentication successful! Redirecting...';
        // Here you could exchange the code for tokens if needed
        // For now, just redirect to a dashboard or home page
        setTimeout(() => this.router.navigate(['/dashboard']), 2000);
      } else {
        this.error = 'No authentication code received';
        setTimeout(() => this.router.navigate(['/login']), 3000);
      }
    });
  }
}
