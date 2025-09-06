import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { API_BASE_URL } from '../security.config';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  googleAuthUrl = `${API_BASE_URL}/auth/oauth2/google`;
  returnUrl: string = '/dashboard';

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    // Check if user is already authenticated
    const token = localStorage.getItem('token');
    if (token) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Get return URL from route parameters or default to '/dashboard'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }
}
