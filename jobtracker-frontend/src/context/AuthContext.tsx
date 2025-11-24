/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-explicit-any */
// src/context/AuthContext.tsx
'use client';

import { createContext, useContext, useEffect, useState, ReactNode, JSX } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';
import { apiClient } from '@/lib/api';
import { useQuery } from '@tanstack/react-query';
import { LoginRequest, RegisterRequest, User } from '@/lib/types';
import toast from 'react-hot-toast';
import { jwtDecode } from 'jwt-decode';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  getCurrentUser: () => Promise<User>;
  logout: () => void;
  beginGoogleLogin: () => void;
  completeGoogleLogin: (accessToken: string,
    profile: { email: string; firstName: string; lastName: string }) => void;
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

export const AuthProvider = ({ children }: { children: ReactNode }): JSX.Element => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Fetch profile if there is a token but no user details
  const { data: profile, isError } = useQuery({
    queryKey: ['currentUser'],
    queryFn: apiClient.getCurrentUser,
    enabled: !!Cookies.get('token') && user === null,
    retry: false,
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
    const initializeAuth = async () => {
      const token = Cookies.get('token');
      if (token) {
        try {
          const payload: any = jwtDecode(token);
          setUser({
            userId: payload.sub || '',
            firstName: payload.firstName || '',
            lastName: payload.lastName || '',
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
    };
    initializeAuth();
  }, []);

  const login = async (data: LoginRequest): Promise<void> => {
    setIsLoading(true);
    try {
      const response = await apiClient.login(data);
      // SECURITY FIX: Always use secure cookies
      // Note: httpOnly cannot be set client-side; consider server-side cookie management for production
      Cookies.set('token', response.token, {
        expires: 7,
        secure: process.env.NODE_ENV === 'production', // Always require HTTPS
        sameSite: 'strict'
      });

      setUser({
        userId: '',
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email || data.email,
        role: response.role || 'USER',
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

  const register = async (data: RegisterRequest): Promise<void> => {
    setIsLoading(true);
    try {
      await apiClient.register(data);
      toast.success('Registration successful! Please check your email to verify your account, then login.');
      router.push('/auth/login');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Registration failed');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const getCurrentUser = async (): Promise<User> => {
    setIsLoading(true);
    try {
      const response = await apiClient.getCurrentUser();
      setUser(response);
      return response;
    } catch (error: any) {
      console.error('Failed to get current user:', error);
      toast.error(error.response?.data?.message || 'Failed to get current user');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const beginGoogleLogin = () => {
    const domain = process.env.NEXT_PUBLIC_AUTH0_DOMAIN;
    const clientId = process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID;
    const callback = encodeURIComponent(`${process.env.NEXT_PUBLIC_FRONTEND_URL}/auth/oauth-callback`);

    const url =
      `https://${domain}/authorize?` +
      `response_type=id_token%20token&` +
      `client_id=${clientId}&` +
      `connection=google-oauth2&` +
      `redirect_uri=${callback}&` +
      `scope=openid%20profile%20email&` +
      `nonce=random123`;

    window.location.href = url;
  };

  const completeGoogleLogin = (
    accessToken: string,
    profile: { email: string; firstName: string; lastName: string }
  ) => {
    try {
      Cookies.set('token', accessToken, { 
        expires: 7,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
      });
      
      const payload: any = jwtDecode(accessToken);

      const newUser: User = {
        userId: payload.sub,
        firstName: payload.given_name,
        lastName: payload.family_name,
        email: payload.email,
        role: 'USER',
        notificationEnabled: true,
        emailNotificationsEnabled: true,
        inAppNotificationsEnabled: true,
      };

      setUser(newUser);

      toast.success('Logged in successfully');
      router.push('/dashboard');
    } catch (error) {
      toast.error('Unable to process Google Login');
      router.push('/auth/login');
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
    beginGoogleLogin,
    completeGoogleLogin,
    getCurrentUser,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};