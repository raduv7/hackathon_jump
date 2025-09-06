import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE_URL } from '../security.config';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  userEmail: string = '';
  userName: string = '';
  userPicture: string = '';
  userProvider: string = '';


  // Account management properties
  googleAccounts: any[] = [];
  facebookAccount: any = null;
  linkedinAccount: any = null;

  // Meeting settings properties
  minutesBeforeMeeting: number = 5;
  isSaving: boolean = false;

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.checkAuthentication();
    this.loadAccountData();
  }

  private loadUserInfo() {
    this.userEmail = localStorage.getItem('user_email') || '';
    this.userName = localStorage.getItem('user_name') || '';
    this.userPicture = localStorage.getItem('user_picture') || '';
    this.userProvider = localStorage.getItem('user_provider') || '';
  }

  private checkAuthentication() {
    const token = localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }


  // Account management methods
  private loadAccountData() {
    // Load Google accounts from localStorage
    const googleAccountsData = localStorage.getItem('google_accounts');
    if (googleAccountsData) {
      try {
        this.googleAccounts = JSON.parse(googleAccountsData);
      } catch (error) {
        console.error('Error parsing Google accounts data:', error);
        this.googleAccounts = [];
      }
    }

    // Load Facebook account from localStorage
    const facebookAccountData = localStorage.getItem('facebook_account');
    if (facebookAccountData) {
      try {
        this.facebookAccount = JSON.parse(facebookAccountData);
      } catch (error) {
        console.error('Error parsing Facebook account data:', error);
        this.facebookAccount = null;
      }
    }

    // Load LinkedIn account from localStorage
    const linkedinAccountData = localStorage.getItem('linkedin_account');
    if (linkedinAccountData) {
      try {
        this.linkedinAccount = JSON.parse(linkedinAccountData);
      } catch (error) {
        console.error('Error parsing LinkedIn account data:', error);
        this.linkedinAccount = null;
      }
    }
  }

  // Google account methods
  signInWithGoogle() {
    // TODO: Implement Google OAuth flow
    console.log('Sign in with Google clicked');
    // For now, add a mock account for demonstration
    const mockGoogleAccount = {
      id: 'google_' + Date.now(),
      name: 'John Doe',
      email: 'john.doe@gmail.com',
      picture: 'https://via.placeholder.com/40x40/4285F4/FFFFFF?text=JD'
    };
    this.googleAccounts.push(mockGoogleAccount);
    this.saveGoogleAccounts();
  }

  removeGoogleAccount(account: any) {
    this.googleAccounts = this.googleAccounts.filter(acc => acc.id !== account.id);
    this.saveGoogleAccounts();
  }

  private saveGoogleAccounts() {
    localStorage.setItem('google_accounts', JSON.stringify(this.googleAccounts));
  }

  // Facebook account methods
  signInWithFacebook() {
    // TODO: Implement Facebook OAuth flow
    console.log('Sign in with Facebook clicked');
    // For now, add a mock account for demonstration
    this.facebookAccount = {
      id: 'facebook_' + Date.now(),
      name: 'Jane Smith',
      email: 'jane.smith@facebook.com',
      picture: 'https://via.placeholder.com/40x40/1877F2/FFFFFF?text=JS'
    };
    this.saveFacebookAccount();
  }

  removeFacebookAccount() {
    this.facebookAccount = null;
    localStorage.removeItem('facebook_account');
  }

  private saveFacebookAccount() {
    if (this.facebookAccount) {
      localStorage.setItem('facebook_account', JSON.stringify(this.facebookAccount));
    }
  }

  // LinkedIn account methods
  signInWithLinkedIn() {
    // TODO: Implement LinkedIn OAuth flow
    console.log('Sign in with LinkedIn clicked');
    // For now, add a mock account for demonstration
    this.linkedinAccount = {
      id: 'linkedin_' + Date.now(),
      name: 'Bob Johnson',
      email: 'bob.johnson@linkedin.com',
      picture: 'https://via.placeholder.com/40x40/0077B5/FFFFFF?text=BJ'
    };
    this.saveLinkedInAccount();
  }

  removeLinkedInAccount() {
    this.linkedinAccount = null;
    localStorage.removeItem('linkedin_account');
  }

  private saveLinkedInAccount() {
    if (this.linkedinAccount) {
      localStorage.setItem('linkedin_account', JSON.stringify(this.linkedinAccount));
    }
  }

  // Meeting settings methods
  saveMeetingSettings() {
    if (this.minutesBeforeMeeting < 1 || this.minutesBeforeMeeting > 60) {
      alert('Please enter a value between 1 and 60 minutes.');
      return;
    }

    this.isSaving = true;
    const headers = this.getAuthHeaders();
    const requestBody = {
      minutes_before_meeting: this.minutesBeforeMeeting
    };

    this.http.post(`${API_BASE_URL}/settings/minutes_before_meeting/` + this.minutesBeforeMeeting, requestBody, { headers })
      .subscribe({
        next: (response) => {
          console.log('Meeting settings saved successfully:', response);
          this.isSaving = false;
          // You could add a success message here
          alert('Settings saved successfully!');
        },
        error: (error) => {
          console.error('Error saving meeting settings:', error);
          this.isSaving = false;
          alert('Error saving settings. Please try again.');
        }
      });
  }
}

