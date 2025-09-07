import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutomationsService, Automation } from '../services/automations.service';

@Component({
  selector: 'app-automations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './automations.component.html',
  styleUrls: ['./automations.component.scss']
})
export class AutomationsComponent implements OnInit {
  automations: Automation[] = [];
  showModal: boolean = false;
  isEditing: boolean = false;
  loading: boolean = false;
  
  currentAutomation: Automation = {
    id: 0,
    title: '',
    automationType: '',
    mediaPlatform: '',
    description: '',
    example: ''
  };

  constructor(private automationsService: AutomationsService) {}

  ngOnInit() {
    this.loadAutomations();
  }

  loadAutomations() {
    this.loading = true;
    this.automationsService.getAllAutomations().subscribe({
      next: (automations) => {
        this.automations = automations;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading automations:', error);
        this.loading = false;
      }
    });
  }

  openCreateModal() {
    this.isEditing = false;
    this.currentAutomation = {
      id: 0,
      title: '',
      automationType: '',
      mediaPlatform: '',
      description: '',
      example: ''
    };
    this.showModal = true;
  }

  openEditModal(automation: Automation) {
    this.isEditing = true;
    this.currentAutomation = { ...automation };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.isEditing = false;
    this.currentAutomation = {
      id: 0,
      title: '',
      automationType: '',
      mediaPlatform: '',
      description: '',
      example: ''
    };
  }

  saveAutomation() {
    if (!this.currentAutomation.title || !this.currentAutomation.automationType || !this.currentAutomation.mediaPlatform || !this.currentAutomation.description) {
      return;
    }

    this.loading = true;

    if (this.isEditing) {
      this.automationsService.updateAutomation(
        this.currentAutomation.id,
        this.currentAutomation.title,
        this.currentAutomation.automationType,
        this.currentAutomation.mediaPlatform,
        this.currentAutomation.description,
        this.currentAutomation.example
      ).subscribe({
        next: (updatedAutomation) => {
          this.closeModal();
          this.loadAutomations(); // Refresh the entire list to ensure consistency
        },
        error: (error) => {
          console.error('Error updating automation:', error);
          this.loading = false;
        }
      });
    } else {
      this.automationsService.createAutomation(
        this.currentAutomation.title,
        this.currentAutomation.automationType,
        this.currentAutomation.mediaPlatform,
        this.currentAutomation.description,
        this.currentAutomation.example
      ).subscribe({
        next: (newAutomation) => {
          this.closeModal();
          this.loadAutomations(); // Refresh the entire list to ensure consistency
        },
        error: (error) => {
          console.error('Error creating automation:', error);
          this.loading = false;
        }
      });
    }
  }

  deleteAutomation(id: number) {
    if (confirm('Are you sure you want to delete this automation?')) {
      this.loading = true;
      this.automationsService.deleteAutomation(id).subscribe({
        next: () => {
          this.loadAutomations(); // Refresh the entire list to ensure consistency
        },
        error: (error) => {
          console.error('Error deleting automation:', error);
          this.loading = false;
        }
      });
    }
  }

  getAutomationTypeDisplay(type: string): string {
    switch (type) {
      case 'POST': return 'Post';
      case 'MARKETING_CONTENT': return 'Marketing Content';
      case 'RECRUITING_CONTENT': return 'Recruiting Content';
      default: return type;
    }
  }

  getPlatformDisplay(platform: string): string {
    switch (platform) {
      case 'EMAIL': return 'Email';
      case 'FACEBOOK': return 'Facebook';
      case 'LINKEDIN': return 'LinkedIn';
      default: return platform;
    }
  }
}
