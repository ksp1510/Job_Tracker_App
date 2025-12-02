"use client";

import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { useRouter } from "next/navigation";

export default function OAuthCallbackPage() {
  const { isLoading, isAuthenticated, error, user } = useAuth0();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;

    if (error) {
      console.error('OAuth error:', error);
      router.push('/auth/login?error=oauth_failed');
      return;
    }

    if (isAuthenticated && user) {
      // Auth0 SDK has already handled everything!
      // Just redirect to dashboard
      setTimeout(() => {
        router.push('/dashboard');
      }, 500);
    } else {
      router.push('/auth/login');
    }
  }, [isLoading, isAuthenticated, error, user, router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
        <p className="text-gray-600">
          {isLoading ? 'Completing sign in...' : 'Redirecting...'}
        </p>
      </div>
    </div>
  );
}