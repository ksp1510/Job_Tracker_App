// src/lib/types.ts

export interface User {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    role: string;
    notificationEnabled: boolean;
    emailNotificationsEnabled: boolean;
    inAppNotificationsEnabled: boolean;
}

// FIXED: Added missing fields
export interface Application {
    id: string;
    userId: string;
    externalJobId: string;
    companyName: string;
    jobTitle: string;
    jobLocation?: string;
    jobDescription?: string;
    jobLink?: string;
    recruiterContact?: string;
    status: ApplicationStatus;
    salary?: string;
    notes?: string;
    resumeId?: string;
    coverLetterId?: string;
    appliedDate?: string;
    lastStatusChangeDate?: string;  // ADDED
    createdAt?: string;              // ADDED
    updatedAt?: string;              // ADDED
    referral?: string;
    interviewDate?: string;
    assessmentDate?: string;
}

export enum ApplicationStatus {
    APPLIED = 'APPLIED',
    INTERVIEW = 'INTERVIEW',
    ASSESSMENT = 'ASSESSMENT',
    OFFER = 'OFFER',
    REJECTED = 'REJECTED',
    WITHDRAWN = 'WITHDRAWN',
}

export interface JobListing {
    id: string;
    userId: string | undefined;
    externalId: string;
    title: string;
    company: string;
    location: string;
    description: string;
    jobType?: string;
    experienceLevel?: string;
    salary?: string;
    salaryRange?: string;
    skills?: string[];
    applyUrl?: string;
    source: string;
    postedDate: string;
    active: boolean;
    saved?: boolean;
}

export interface SavedJob {
    id: string;
    userId: string;
    jobListingId: string;
    savedAt: string;
    notes?: string;
    applied?: boolean;           // ADDED
    applicationId?: string;      // ADDED
}

export interface Notification {
    id: string;
    userId: string;
    applicationId?: string;
    message: string;
    notifyAt: string;
    type: NotificationType;
    channel: NotificationChannel;
    read: boolean;
    sent: boolean;
    createdAt: string;
}

// FIXED: Match backend enum exactly
export enum NotificationType {
    FOLLOW_UP = 'FOLLOW_UP',
    INTERVIEW = 'INTERVIEW',
    DEADLINE = 'DEADLINE',
    CUSTOM = 'CUSTOM',              // ADDED
    STATUS_CHANGE = 'STATUS_CHANGE' // ADDED
}

export enum NotificationChannel {
    EMAIL = 'EMAIL',
    IN_APP = 'IN_APP'
}

export interface AuthResponse {
    token: string;
    role: string;
    firstName: string;
    lastName: string;
}

export interface RegisterRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface ApiResponse<T> {
    data: T;
    status: string;
    message?: string;
}

// FIXED: Added 'page' as alias for 'number' to handle Spring Page response
export interface PaginatedResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    page: number;      // Current page
    number?: number;   // Spring Page uses 'number' 
    size: number;
}

export interface JobSearchParams {
    query?: string;
    location?: string;
    jobType?: string;
    minSalary?: number;
    maxSalary?: number;
    skills?: string[];
    page?: number;
    size?: number;
}

export interface FileUpload {
    id: string;
    applicationId: string;
    userId: string;
    fileName: string;
    type: string;
    filePath: string;
    uploadedAt: string;
    notes?: string;
}