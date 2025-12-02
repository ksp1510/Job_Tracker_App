// src/lib/auth.ts (PRODUCTION-READY VERSION with environment variables)

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Send password reset email
 * @param email - User's email address
 * @returns Promise with success/error message
 */
export async function sendPasswordResetEmail(email: string): Promise<{
  success: boolean;
  message: string;
}> {
  try {
    const response = await fetch(`${API_URL}/auth/forgot-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Failed to send reset email');
    }

    return {
      success: true,
      message: data.message || 'Password reset email sent successfully',
    };
  } catch (error) {
    console.error('Password reset error:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Network error occurred',
    };
  }
}

// Type for forgot password request
export interface ForgotPasswordRequest {
  email: string;
}

// Type for forgot password response
export interface ForgotPasswordResponse {
  message: string;
  success: boolean;
}