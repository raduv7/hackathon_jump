import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ActivatedRoute, Params, Router} from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './callback.component.html',
  styleUrls: ['./callback.component.scss']
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
      // Log all received parameters from the backend
      console.log('=== CALLBACK COMPONENT - QUERY PARAMETERS ===');
      console.log('Full query parameters received:', params);
      console.log('Individual parameters:');
      console.log('- token:', params['token']);
      console.log('- email:', params['email']);
      console.log('- name:', params['name']);
      console.log('- provider:', params['provider']);
      console.log('- error:', params['error']);
      console.log('- All other params:', Object.keys(params).filter(key =>
        !['token', 'email', 'name', 'provider', 'error', 'picture'].includes(key)
      ).reduce((obj, key) => {
        obj[key] = params[key];
        return obj;
      }, {} as any));
      console.log('=== END QUERY PARAMETERS LOG ===');

      const token = params['token'];
      const error = params['error'];

      if (error) {
        this.error = `Authentication failed: ${error}`;
        console.error('Authentication error received:', error);
        setTimeout(() => this.router.navigate(['/login']), 5000);
      } else if (token) {
        this.success = 'Authentication successful! Storing credentials...';
        console.log('Authentication successful, storing credentials...');

        this.updateLocalStorageWithAccountDetails(params);
        this.updateLocalStorageWithToken(params);

        console.log('Redirecting to dashboard...');
        setTimeout(() => this.router.navigate(['/settings']), 2000);
      } else {
        this.error = 'No authentication token received';
        console.error('No authentication token received in callback');
        setTimeout(() => this.router.navigate(['/login']), 3000);
      }
    });
  }

  private updateLocalStorageWithAccountDetails(params: Params): void {
    const provider = params['provider'];
    if(provider == 'GOOGLE') {
      this.updateLocalStorageWithGoogleDetails(params);
    } else if(provider == 'FACEBOOK') {
      this.updateLocalStorageWithFacebookDetails(params);
    } else if(provider == 'LINKEDIN') {
      this.updateLocalStorageWithLinkedinDetails(params);
    } else {
      console.error("unsupported provider: ", provider);
    }
  }

  private updateLocalStorageWithGoogleDetails(params: Params): void {
    const email = params['email'];
    let googleEmailList = localStorage.getItem('googleEmailList');
    if(googleEmailList == null || googleEmailList.length == 0) {
      googleEmailList = email;
    } else {
      googleEmailList = googleEmailList + ',' + email;
    }
    localStorage.setItem('googleEmailList', googleEmailList == null ? '' : googleEmailList);

    const name = params['name'];
    let googleNameList = localStorage.getItem('googleNameList');
    if(googleNameList == null || googleNameList.length == 0) {
      googleNameList = name;
    } else {
      googleNameList = googleNameList + ',' + name;
    }
    localStorage.setItem('googleNameList', googleNameList == null ? '' : googleNameList);
  }

  private updateLocalStorageWithFacebookDetails(params: Params): void {
    localStorage.setItem('facebookUsername', params['username']);
    localStorage.setItem('facebookName', params['name']);
  }

  private updateLocalStorageWithLinkedinDetails(params: Params): void {
    localStorage.setItem('linkedinUsername', params['username']);
    localStorage.setItem('linkedinName', params['name']);
  }

  private updateLocalStorageWithToken(params: Params): void {
    let existingToken = localStorage.getItem('token');
    if(existingToken == null || existingToken.length == 0) {
      localStorage.setItem('token', params['token']);
    } else {
      // Merge tokens asynchronously
      this.mergeTokens(existingToken, params['token']);
    }
  }

  private mergeTokens(token1: string, token2: string): void {
    // Call backend to merge tokens
    const headers = {
      'Authorization': `Bearer ${token1}`,
      'Content-Type': 'application/json'
    };

    this.http.post<string>('http://localhost:8080/auth/tokens', token2, { headers })
      .subscribe({
        next: (mergedToken) => {
          console.log('Tokens merged successfully');
          // Save the merged token to localStorage
          localStorage.setItem('token', mergedToken);
        },
        error: (error) => {
          console.error('Failed to merge tokens:', error);
          // Keep the first token if merge fails
          console.log('Keeping existing token due to merge failure');
        }
      });

    setTimeout(() => this.router.navigate(['/settings']), 2000);
  }
}
