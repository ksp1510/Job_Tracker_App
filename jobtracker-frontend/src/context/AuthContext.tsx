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
import { useAuth0 } from '@auth0/auth0-react';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  getCurrentUser: () => Promise<User>;
  logout: () => void;
  beginGoogleLogin: () => void;
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

  const {
    isAuthenticated: auth0IsAuthenticated,
    isLoading: auth0IsLoading,
    user: auth0User,
    loginWithRedirect,
    logout: auth0Logout,
    getAccessTokenSilently,    
  } = useAuth0();

  useEffect(() => {
    const syncAuth0User = async () => {
      if (auth0IsAuthenticated && auth0User) {
        try {
          const token = await getAccessTokenSilently({
            authorizationParams: {
              audience: process.env.NEXT_PUBLIC_AUTH0_AUDIENCE,
            },
          });

          Cookies.set('token', token, {
            expires: 7,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
          });

          const mappedUser: User = {
            userId: auth0User.sub!,
            firstName: auth0User.given_name || auth0User.name?.split(' ')[0] || 'User',
            lastName: auth0User.family_name || auth0User.name?.split(' ')[1] || '',
            email: auth0User.email!,
            role: 'USER',
            notificationEnabled: true,
            emailNotificationsEnabled: true,
            inAppNotificationsEnabled: true,
          };

          setUser(mappedUser);
          toast.success(`Welcome, ${mappedUser.firstName}!`);
          router.push('/dashboard');
        } catch (error) {
          console.error('Error getting Auth0 token:', error);
          toast.error('Unable to process Google Login');
          router.push('/auth/login');
        }
      }
    };

    if (!auth0IsLoading && auth0IsAuthenticated) {
      syncAuth0User();
    }
  }, [auth0IsAuthenticated, auth0User, auth0IsLoading, getAccessTokenSilently]);

  // Fetch profile if there is a token but no user details
  const { data: profile, isError } = useQuery({
    queryKey: ['currentUser'],
    queryFn: apiClient.getCurrentUser,
    enabled: !!Cookies.get('token') && user === null && !auth0IsAuthenticated,
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
      if (auth0IsAuthenticated) {
        setIsLoading(true);
        return;
      }

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

    if (!auth0IsLoading) {
      initializeAuth();
    }
  }, [auth0IsLoading, auth0IsAuthenticated]);

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
    loginWithRedirect({
      authorizationParams: {
        connection: "google-oauth2",
        audience: process.env.NEXT_PUBLIC_AUTH0_AUDIENCE,
        scope: 'openid profile email',
      },
    });
  };

  const logout = (): void => {
    Cookies.remove('token');
    setUser(null);

    if (auth0IsAuthenticated) {
      auth0Logout({
        logoutParams: {
          returnTo: `${window.location.origin}/auth/login`,
        },
      });
    } else {
      toast.success('Logged out successfully');
      router.push('/auth/login');
    }
  };

  const value: AuthContextType = {
    user,
    isLoading: isLoading || auth0IsLoading,
    login,
    register,
    logout,
    beginGoogleLogin,
    getCurrentUser,
    isAuthenticated: !!user || auth0IsAuthenticated,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};