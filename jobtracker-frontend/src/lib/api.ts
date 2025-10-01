// src/lib/api.ts
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import Cookies from 'js-cookie';
import { 
  AuthResponse, 
  LoginRequest, 
  RegisterRequest, 
  Application, 
  JobListing, 
  SavedJob, 
  Notification, 
  JobSearchParams,
  PaginatedResponse,
  User 
} from './types';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: process.env.NEXT_PUBLIC_API_URL,
      timeout: 10000,
    });

    console.log('API URL:', process.env.NEXT_PUBLIC_API_URL);

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        const token = Cookies.get('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Clear token and redirect to login
          Cookies.remove('token');
          window.location.href = '/auth/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth endpoints
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await this.client.post('/auth/register', data);
    return response.data;
  }

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await this.client.post('/auth/login', data);
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get('/users/me');
    return response.data;
  }

  // Application endpoints
  async getApplications(): Promise<Application[]> {
    const response = await this.client.get('/applications');
    return response.data;
  }

  async getApplicationsByStatus(status: string): Promise<Application[]> {
    const response = await this.client.get(`/applications/by-status?status=${status}`);
    return response.data;
  }

  async getApplication(id: string): Promise<Application> {
    const response = await this.client.get(`/applications/${id}`);
    return response.data;
  }

  async createApplication(data: Omit<Application, 'id' | 'userId'>): Promise<Application> {
    const response = await this.client.post('/applications', data);
    return response.data;
  }

  async updateApplication(id: string, data: Partial<Application>): Promise<Application> {
    const response = await this.client.put(`/applications/${id}`, data);
    return response.data;
  }

  async deleteApplication(id: string): Promise<void> {
    await this.client.delete(`/applications/${id}`);
  }

  // Job search endpoints
  async searchJobs(params: JobSearchParams): Promise<PaginatedResponse<JobListing>> {
    const response = await this.client.get('/jobs/search', { params });
    return response.data;
  }

  async getJob(id: string): Promise<JobListing> {
    const response = await this.client.get(`/jobs/${id}`);
    return response.data;
  }

  async saveJob(id: string, notes?: string): Promise<SavedJob> {
    const response = await this.client.post(`/jobs/${id}/save`, { notes });
    return response.data;
  }

  async getSavedJobs(): Promise<SavedJob[]> {
    const response = await this.client.get('/jobs/saved');
    return response.data;
  }

  async unsaveJob(id: string): Promise<void> {
    await this.client.delete(`/jobs/saved/${id}`);
  }

  // Notification endpoints
  async getNotifications(): Promise<Notification[]> {
    const response = await this.client.get('/notifications');
    return response.data;
  }

  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await this.client.get('/notifications/unread');
    return response.data;
  }

  async markNotificationAsRead(id: string): Promise<Notification> {
    const response = await this.client.patch(`/notifications/${id}/read`);
    return response.data;
  }

  async deleteNotification(id: string): Promise<void> {
    await this.client.delete(`/notifications/${id}`);
  }

  async createInterviewReminder(data: {
    applicationId: string;
    interviewDate: string;
    customMessage?: string;
  }): Promise<Notification> {
    const response = await this.client.post('/notifications/interview-reminder', data);
    return response.data;
  }

  async createDeadlineReminder(data: {
    applicationId: string;
    assessmentDeadline: string;
    customMessage?: string;
  }): Promise<Notification> {
    const response = await this.client.post('/notifications/deadline-reminder', data);
    return response.data;
  }

  async createCustomNotification(data: {
    applicationId?: string;
    message: string;
    notifyAt: string;
    type?: string;
    channel?: string;
  }): Promise<Notification> {
    const response = await this.client.post('/notifications/custom', data);
    return response.data;
  }

  // File upload endpoints
  async uploadResume(applicationId: string, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('applicationId', applicationId);

    const response = await this.client.post('/files/upload/resume', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async uploadCoverLetter(applicationId: string, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('applicationId', applicationId);

    const response = await this.client.post('/files/upload/coverletter', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async deleteFile(id: string): Promise<string> {
    const response = await this.client.delete(`/files/delete/${id}`);
    return response.data;
  }

  async getPresignedUrl(id: string): Promise<string> {
    const response = await this.client.get(`/files/presigned/${id}`);
    return response.data;
  }
}

export const apiClient = new ApiClient();