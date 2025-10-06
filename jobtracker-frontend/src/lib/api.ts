/* eslint-disable @typescript-eslint/no-explicit-any */
// src/lib/api.ts
import axios, { AxiosInstance } from 'axios';
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

    // Request interceptor
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

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
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

  async createApplication(data: Omit<Application, 'id'>): Promise<Application> {
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

  // ============================================
  // JOB SEARCH ENDPOINTS - FIXED
  // ============================================

  /**
   * Check cache status
   */
  async checkCacheStatus(): Promise<boolean> {
    try {
      const response = await this.client.get('/jobs/cache/status');
      return response.data.hasCachedResults;
    } catch (error) {
      console.error('Failed to check cache status:', error);
      return false;
    }
  }

  /**
   * Get cached results - FIXED: Handle 204 No Content response
   */
  async getCachedResults(page = 0, size = 10): Promise<PaginatedResponse<JobListing> | null> {
    try {
      const response = await this.client.get('/jobs/cache', { params: { page, size } });
      return this.normalizePaginationResponse(response.data);
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 204) {
        return null;
      }
      throw error;
    }
  }

  /**
   * Search jobs - FIXED: Normalize pagination response
   */
  async searchJobs(params: JobSearchParams): Promise<PaginatedResponse<JobListing>> {
    const response = await this.client.get('/jobs/search', { params });
    return this.normalizePaginationResponse(response.data);
  }

  /**
   * FIXED: Helper to normalize Spring Page response to our interface
   */
  private normalizePaginationResponse(data: any): PaginatedResponse<JobListing> {
    return {
      content: data.content || [],
      totalElements: data.totalElements || 0,
      totalPages: data.totalPages || 0,
      page: data.page !== undefined ? data.page : (data.number || 0),
      size: data.size || 10,
    };
  }

  /**
   * Clear cache
   */
  async clearCache(): Promise<void> {
    await this.client.delete('/jobs/cache');
  }

  /**
   * Get job by ID
   */
  async getJob(id: string): Promise<JobListing> {
    const response = await this.client.get(`/jobs/${id}`);
    return response.data;
  }

  /**
   * Save job - FIXED: Removed userId from request body
   */
  async saveJob(id: string, notes?: string): Promise<SavedJob> {
    const response = await this.client.post(`/jobs/${id}/save`, { notes });
    return response.data;
  }

  /**
   * Get saved jobs
   */
  async getSavedJobs(): Promise<SavedJob[]> {
    const response = await this.client.get('/jobs/saved');
    return response.data;
  }

  /**
   * Unsave job
   */
  async unsaveJob(id: string): Promise<void> {
    await this.client.delete(`/jobs/saved/${id}`);
  }

  /**
   * Get search history
   */
  async getSearchHistory(): Promise<any[]> {
    const response = await this.client.get('/jobs/search-history');
    return response.data;
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

  /**
   * Create interview reminder - FIXED
   */
  async createInterviewReminder(data: {
    applicationId: string;
    interviewDate: string;
    customMessage?: string;
  }): Promise<Notification> {
    const response = await this.client.post('/notifications/interview-reminder', data);
    return response.data;
  }

  /**
   * Create deadline reminder - FIXED
   */
  async createDeadlineReminder(data: {
    applicationId: string;
    assessmentDeadline: string;
    customMessage?: string;
  }): Promise<Notification> {
    const response = await this.client.post('/notifications/deadline-reminder', data);
    return response.data;
  }

  /**
   * Create custom notification - FIXED
   */
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