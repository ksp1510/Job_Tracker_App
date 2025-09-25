/* eslint-disable @typescript-eslint/no-explicit-any */
// src/context/AuthContext.tsx
'use client';

import { createContext, useContext, useEffect, useState, ReactNode, JSX } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';
import { apiClient } from '@/lib/api';
import { LoginRequest, RegisterRequest, User } from '@/lib/types';
import toast from 'react-hot-toast';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps): JSX.Element => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Check if user is authenticated on mount
  useEffect(() => {
    const token = Cookies.get('token');
    if (token) {
      // In a real app, you might want to validate the token with the server
      // For now, we'll just assume it's valid and set a basic user object
      try {
        // Decode JWT payload (basic implementation)
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
          userId: payload.sub,
          firstName: 'User', // These would come from a user profile API call
          lastName: 'Name',
          email: payload.email || '',
          role: payload.role || 'USER',
          notificationEnabled: true,
          emailNotificationsEnabled: true,
          inAppNotificationsEnabled: true,
        });
      } catch (error) {
        console.error('Invalid token:', error);
        Cookies.remove('token');
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (data: LoginRequest): Promise<void> => {
    try {
      setIsLoading(true);
      const response = await apiClient.login(data);
      
      // Store token in cookie
      Cookies.set('token', response.token, { 
        expires: 7, // 7 days
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict'
      });

      // Decode token to get user info
      const payload = JSON.parse(atob(response.token.split('.')[1]));
      setUser({
        userId: payload.sub,
        firstName: 'User',
        lastName: 'Name', 
        email: data.email,
        role: response.role,
        notificationEnabled: true,
        emailNotificationsEnabled: true,
        inAppNotificationsEnabled: true,
      });

      toast.success('Login successful!');
      router.push('/dashboard');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Login failed');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<void> => {
    try {
      setIsLoading(true);
      await apiClient.register(data);
      toast.success('Registration successful! Please login.');
      router.push('/auth/login');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Registration failed');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = (): void => {
    Cookies.remove('token');
    setUser(null);
    toast.success('Logged out successfully');
    router.push('/auth/login');
  };

  const value: AuthContextType = {
    user,
    isLoading,
    login,
    register,
    logout,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};