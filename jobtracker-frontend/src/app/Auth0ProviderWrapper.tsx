'use client';

import { Auth0Provider } from '@auth0/auth0-react';

export default function Auth0ProviderWrapper({ children }: { children: React.ReactNode }) {

  console.log('🔑 Auth0 Client ID:', process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID);
  console.log('🌐 Auth0 Domain:', process.env.NEXT_PUBLIC_AUTH0_DOMAIN);
  console.log('🎯 Auth0 Audience:', process.env.NEXT_PUBLIC_AUTH0_AUDIENCE);

  
  return (
    <Auth0Provider
      domain={process.env.NEXT_PUBLIC_AUTH0_DOMAIN!}
      clientId={process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID!}
      authorizationParams={{
        redirect_uri: typeof window !== 'undefined' 
          ? `${window.location.origin}/auth/oauth-callback`
          : '',
        audience: process.env.NEXT_PUBLIC_AUTH0_AUDIENCE,  
        scope: 'openid profile email',  
      }}
      cacheLocation="localstorage" 
      useRefreshTokens={true} 
    >
      {children}
    </Auth0Provider>
  );
}
