/* eslint-disable @typescript-eslint/no-explicit-any */
// src/context/AuthContext.tsx
'use client';

import { createContext, useContext, useEffect, useState, ReactNode, JSX } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';
import { apiClient } from '@/lib/api';
import { AuthResponse } from '@/lib/types';
import { useQuery } from '@tanstack/react-query';
import { LoginRequest, RegisterRequest, User } from '@/lib/types';
import toast from 'react-hot-toast';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  getCurrentUser: () => Promise<User>;
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

  // Fetch profile if there is a token but no user details
  const { data: profile, isError } = useQuery({
    queryKey: ['currentUser'],
    queryFn: apiClient.getCurrentUser,
    enabled: !!Cookies.get('token') && user === null,
  });
  useEffect(() => {
    if (profile) setUser(profile);
  }, [profile]);

  useEffect(() => {
    if (isError) {
      Cookies.remove('token');
      setUser(null);
    }
  }, [isError]);
  
  // On mount decode token to set minimal user info
  useEffect(() => {
    const token = Cookies.get('token');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      setUser((prev) => ({
        userId: payload.sub,
        firstName: payload.firstName ?? prev?.firstName ?? '',
        lastName: payload.lastName ?? prev?.lastName ?? '',
        email: payload.email ?? '',
        role: payload.role ?? 'USER',
        notificationEnabled: true,
        emailNotificationsEnabled: true,
        inAppNotificationsEnabled: true,
      }));
    }
    setIsLoading(false);
  }, []);

  const login = async (data: LoginRequest): Promise<void> => {
    setIsLoading(true);
    try {
      const response = await apiClient.login(data);
      // SECURITY FIX: Always use secure cookies
      // Note: httpOnly cannot be set client-side; consider server-side cookie management for production
      Cookies.set('token', response.token, {
        expires: 7,
        secure: true, // Always require HTTPS
        sameSite: 'strict'
      });

      setUser({
        userId: JSON.parse(atob(response.token.split('.')[1])).sub,
        firstName: response.firstName,
        lastName: response.lastName,
        email: data.email,
        role: response.role,
        notificationEnabled: true,
        emailNotificationsEnabled: true,
        inAppNotificationsEnabled: true,
      });

      toast.success('Login successful!');
      console.log(user?.firstName);
      router.push('/dashboard');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Login failed');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

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

  const getCurrentUser = async (): Promise<User> => {
    try {
      setIsLoading(true);
      const response = await apiClient.getCurrentUser();
      setUser(response);
      return response;
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to get current user');
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
    getCurrentUser,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};