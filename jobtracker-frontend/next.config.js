/** @type {import('next').NextConfig} */
const nextConfig = {
  // ------------------------------------------------------
  // Image Optimization (allow EC2 IP + domains)
  // ------------------------------------------------------
  images: {
    domains: [
      "localhost",
      "127.0.0.1",
      "100.31.153.218",                // ← EC2 PUBLIC IP
      "app.careertrackr.dev",
      "api.careertrackr.dev",
      "d1re9c8ele1co.cloudfront.net",  // ← CLOUD FRONT DOMAIN
    ],
    formats: ["image/avif", "image/webp"],
  },

  // ------------------------------------------------------
  // Public Environment Vars (Browser-exposed)
  // These match your `.env.production`
  // ------------------------------------------------------
  env: {
    NEXT_PUBLIC_API_URL:
      process.env.NEXT_PUBLIC_API_URL ||
      "https://api.careertrackr.dev",

    NEXT_PUBLIC_FRONTEND_URL:
      process.env.NEXT_PUBLIC_FRONTEND_URL ||
      "https://app.careertrackr.dev",

    NEXT_PUBLIC_AUTH0_DOMAIN:
      process.env.NEXT_PUBLIC_AUTH0_DOMAIN ||
      "dev-ef81elo8soa00i4u.ca.auth0.com",

    NEXT_PUBLIC_AUTH0_CLIENT_ID:
      process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID ||
      "CFIx0o9sJIqW2y9AGNnbzNNS8NXeJWFZ",

    NEXT_PUBLIC_AUTH0_AUDIENCE:
      process.env.NEXT_PUBLIC_AUTH0_AUDIENCE ||
      "https://jobtracker-api",
  },

  // ------------------------------------------------------
  // Fix: Turbopack "workspace root" warning
  // ------------------------------------------------------
  experimental: {
    serverActions: {
      allowedOrigins: [
        "localhost",
        "127.0.0.1",
        "100.31.153.218",       
        "app.careertrackr.dev",
        "api.careertrackr.dev",
        "d1re9c8ele1co.cloudfront.net",
      ],
    },
  },

  // ------------------------------------------------------
  // Production Hardening & Performance
  // ------------------------------------------------------
  poweredByHeader: false,
  reactStrictMode: true,
  compress: true,
  generateEtags: false, // better compatibility behind CloudFront
};

module.exports = nextConfig;
