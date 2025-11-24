'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';
import { jwtDecode } from 'jwt-decode';
import { useAuth } from '@/context/AuthContext';

type IdTokenPayload = {
  email: string;
  given_name?: string;
  family_name?: string;
  name?: string;
}

export default function Auth0CallbackPage() {
  const router = useRouter();
  const { completeGoogleLogin } = useAuth();

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const hash = window.location.hash.startsWith('#')
      ? window.location.hash.substring(1)
      : window.location.hash;

    const params = new URLSearchParams(hash);
    const accessToken = params.get('access_token');
    const idToken = params.get('id_token');
    const error = params.get('error');
    const errorDescription = params.get('error_description');

    if (error) {
      console.error('Auth0 error:', error, errorDescription);
      toast.error(errorDescription || 'Authentication failed');
      router.replace('/auth/login');
      return;
    }

    if (!accessToken || !idToken) {
      toast.error('Authentication failed - missing token');
      router.replace('/auth/login');
      return;
    }

    try {
      const payload = jwtDecode<IdTokenPayload>(idToken);

      const profile = {
        email: payload.email,
        firstName:
          payload.given_name ??
          (payload.name ? payload.name.split(' ')[0] : 'User'),
        lastName:
          payload.family_name ??
          (payload.name && payload.name.split(' ').length > 1
            ? payload.name.split(' ').slice(1).join(' ')
            : ''),
      };

      // AuthContext handles setting cookie + redirect
      completeGoogleLogin(accessToken, profile);
    } catch (err) {
      console.error('Failed to decode ID token', err);
      toast.error('Authentication failed.');
      router.replace('/auth/login');
    }
  }, [router, completeGoogleLogin]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">Completing sign in...</p>
      </div>
    </div>
  );
}
