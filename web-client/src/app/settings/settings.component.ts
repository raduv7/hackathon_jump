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
    this.loadAccountData();
    this.loadMinutesBeforeMeeting();
  }

  private loadUserInfo() {
    this.userEmail = localStorage.getItem('user_email') || '';
    this.userName = localStorage.getItem('user_name') || '';
    this.userPicture = localStorage.getItem('user_picture') || '';
    this.userProvider = localStorage.getItem('user_provider') || '';
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
    // Load Google accounts from localStorage using googleEmailList and googleNameList
    this.loadGoogleAccountsFromLists();

    // Load Facebook account from localStorage - check for facebookUsername
    const facebookUsername = localStorage.getItem('facebookUsername');
    
    if (facebookUsername) {
      this.facebookAccount = {
        id: 'facebook_' + Date.now(),
        username: facebookUsername,
        name: facebookUsername, // Use username as display name
        email: null, // Facebook doesn't provide email in this flow
        picture: null // Will use Facebook logo instead
      };
    } else {
      // Fallback to old facebook_account format if it exists
      const facebookAccountData = localStorage.getItem('facebook_account');
      if (facebookAccountData) {
        try {
          this.facebookAccount = JSON.parse(facebookAccountData);
        } catch (error) {
          console.error('Error parsing Facebook account data:', error);
          this.facebookAccount = null;
        }
      } else {
        this.facebookAccount = null;
      }
    }

    // Load LinkedIn account from localStorage - check for linkedinUsername and linkedinName
    const linkedinUsername = localStorage.getItem('linkedinUsername');
    const linkedinName = localStorage.getItem('linkedinName');

    if (linkedinUsername && linkedinName) {
      this.linkedinAccount = {
        id: 'linkedin_' + Date.now(),
        username: linkedinUsername,
      };
    } else {
      // Fallback to old linkedin_account format if it exists
      const linkedinAccountData = localStorage.getItem('linkedin_account');
      if (linkedinAccountData) {
        try {
          this.linkedinAccount = JSON.parse(linkedinAccountData);
        } catch (error) {
          console.error('Error parsing LinkedIn account data:', error);
          this.linkedinAccount = null;
        }
      } else {
        this.linkedinAccount = null;
      }
    }
  }

  // Google account methods
  signInWithGoogle() {
    // Redirect to Google OAuth flow
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
  }


  private loadGoogleAccountsFromLists() {
    const googleEmailList = localStorage.getItem('googleEmailList');
    const googleNameList = localStorage.getItem('googleNameList');

    this.googleAccounts = [];

    if (googleEmailList && googleNameList) {
      const emails = googleEmailList.split(',').filter(email => email.trim() !== '');
      const names = googleNameList.split(',').filter(name => name.trim() !== '');

      // Ensure both lists have the same length
      const minLength = Math.min(emails.length, names.length);

      for (let i = 0; i < minLength; i++) {
        this.googleAccounts.push({
          id: `google_${i}`,
          name: names[i].trim(),
          email: emails[i].trim(),
          picture: null // No profile picture available, will use Google logo
        });
      }
    }
  }


  // Facebook account methods
  signInWithFacebook() {
    // Redirect to Facebook OAuth flow
    window.location.href = `${API_BASE_URL}/oauth2/authorization/facebook`;
  }

  private saveFacebookAccount() {
    if (this.facebookAccount) {
      localStorage.setItem('facebook_account', JSON.stringify(this.facebookAccount));
    }
  }

  // LinkedIn account methods
  signInWithLinkedIn() {
    // Redirect to LinkedIn OAuth flow
    window.location.href = `${API_BASE_URL}/oauth2/authorization/linkedin`;
  }

  private saveLinkedInAccount() {
    if (this.linkedinAccount) {
      localStorage.setItem('linkedin_account', JSON.stringify(this.linkedinAccount));
    }
  }

  // Meeting settings methods
  private loadMinutesBeforeMeeting() {
    const headers = this.getAuthHeaders();
    
    this.http.get<number>(`${API_BASE_URL}/settings/minutes_before_meeting`, { headers })
      .subscribe({
        next: (minutes) => {
          this.minutesBeforeMeeting = minutes;
          console.log('Loaded minutes before meeting:', minutes);
        },
        error: (error) => {
          console.error('Error loading minutes before meeting:', error);
          // Keep the default value of 5 if loading fails
        }
      });
  }

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
          // Reload the value from server to ensure consistency
          this.loadMinutesBeforeMeeting();
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

