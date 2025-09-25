/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react/no-unescaped-entities */
// src/app/dashboard/page.tsx
'use client';

import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Application, ApplicationStatus } from '@/lib/types';
import Link from 'next/link';
import {
  DocumentDuplicateIcon,
  MagnifyingGlassIcon,
  CalendarIcon,
  ClockIcon,
  ChartBarIcon,
  PlusIcon,
} from '@heroicons/react/24/outline';
import { formatDate, getStatusColor } from '@/lib/utils';

export default function DashboardPage() {
  const { user, isAuthenticated } = useAuth();

  // Fetch applications
  const { data: applications = [], isLoading } = useQuery({
    queryKey: ['applications'],
    queryFn: () => apiClient.getApplications(),
    enabled: isAuthenticated,
  });

  // Fetch recent notifications
  const { data: notifications = [] } = useQuery({
    queryKey: ['unread-notifications'],
    queryFn: () => apiClient.getUnreadNotifications(),
    enabled: isAuthenticated,
  });

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  // Calculate statistics
  const stats = {
    total: applications.length,
    applied: applications.filter((app: Application) => app.status === ApplicationStatus.APPLIED).length,
    interview: applications.filter((app: Application) => app.status === ApplicationStatus.INTERVIEW).length,
    offers: applications.filter((app: Application) => app.status === ApplicationStatus.OFFER).length,
    rejected: applications.filter((app: Application) => app.status === ApplicationStatus.REJECTED).length,
  };

  // Recent applications (last 5)
  const recentApplications = applications
    .sort((a: Application, b: Application) => 
      new Date(b.appliedDate || 0).getTime() - new Date(a.appliedDate || 0).getTime()
    )
    .slice(0, 5);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <h1 className="text-2xl font-bold text-gray-900">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="mt-1 text-gray-600">
            Here's an overview of your job search progress.
          </p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <DocumentDuplicateIcon className="h-6 w-6 text-gray-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total Applications
                    </dt>
                    <dd className="text-lg font-medium text-gray-900">{stats.total}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <ClockIcon className="h-6 w-6 text-blue-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Applied
                    </dt>
                    <dd className="text-lg font-medium text-gray-900">{stats.applied}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <CalendarIcon className="h-6 w-6 text-yellow-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Interviews
                    </dt>
                    <dd className="text-lg font-medium text-gray-900">{stats.interview}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <ChartBarIcon className="h-6 w-6 text-green-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Offers
                    </dt>
                    <dd className="text-lg font-medium text-gray-900">{stats.offers}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white shadow rounded-lg mb-8">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Quick Actions
            </h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Link
                href="/applications/new"
                className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
              >
                <div>
                  <span className="rounded-lg inline-flex p-3 bg-indigo-50 text-indigo-700 ring-4 ring-white">
                    <PlusIcon className="h-6 w-6" />
                  </span>
                </div>
                <div className="mt-4">
                  <h3 className="text-lg font-medium text-gray-900">
                    Add Application
                  </h3>
                  <p className="mt-2 text-sm text-gray-500">
                    Record a new job application you've submitted
                  </p>
                </div>
              </Link>

              <Link
                href="/jobs"
                className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
              >
                <div>
                  <span className="rounded-lg inline-flex p-3 bg-blue-50 text-blue-700 ring-4 ring-white">
                    <MagnifyingGlassIcon className="h-6 w-6" />
                  </span>
                </div>
                <div className="mt-4">
                  <h3 className="text-lg font-medium text-gray-900">
                    Search Jobs
                  </h3>
                  <p className="mt-2 text-sm text-gray-500">
                    Find new job opportunities to apply to
                  </p>
                </div>
              </Link>

              <Link
                href="/notifications"
                className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
              >
                <div>
                  <span className="rounded-lg inline-flex p-3 bg-yellow-50 text-yellow-700 ring-4 ring-white">
                    <CalendarIcon className="h-6 w-6" />
                  </span>
                </div>
                <div className="mt-4">
                  <h3 className="text-lg font-medium text-gray-900">
                    Set Reminders
                  </h3>
                  <p className="mt-2 text-sm text-gray-500">
                    Create reminders for interviews and follow-ups
                  </p>
                </div>
              </Link>
            </div>
          </div>
        </div>

        {/* Recent Applications and Notifications */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {/* Recent Applications */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Recent Applications
                </h3>
                <Link
                  href="/applications"
                  className="text-sm font-medium text-indigo-600 hover:text-indigo-500"
                >
                  View all
                </Link>
              </div>
              
              {isLoading ? (
                <div className="animate-pulse space-y-4">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="flex items-center space-x-4">
                      <div className="h-10 w-10 bg-gray-200 rounded"></div>
                      <div className="flex-1">
                        <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                        <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : recentApplications.length === 0 ? (
                <div className="text-center py-6">
                  <DocumentDuplicateIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No applications yet</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    Get started by creating your first application.
                  </p>
                  <div className="mt-6">
                    <Link
                      href="/applications/new"
                      className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                    >
                      <PlusIcon className="-ml-1 mr-2 h-5 w-5" />
                      Add Application
                    </Link>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  {recentApplications.map((application: Application) => (
                    <div
                      key={application.id}
                      className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
                    >
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <h4 className="text-sm font-medium text-gray-900">
                            {application.jobTitle}
                          </h4>
                          <span
                            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(
                              application.status
                            )}`}
                          >
                            {application.status}
                          </span>
                        </div>
                        <p className="text-sm text-gray-500 mt-1">
                          {application.companyName}
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                          Applied: {formatDate(application.appliedDate || '')}
                        </p>
                      </div>
                      <Link
                        href={`/applications/${application.id}`}
                        className="ml-4 text-indigo-600 hover:text-indigo-500 text-sm font-medium"
                      >
                        View
                      </Link>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Recent Notifications */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Recent Notifications
                </h3>
                <Link
                  href="/notifications"
                  className="text-sm font-medium text-indigo-600 hover:text-indigo-500"
                >
                  View all
                </Link>
              </div>
              
              {notifications.length === 0 ? (
                <div className="text-center py-6">
                  <ClockIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No notifications</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    You're all caught up! Set up reminders to stay organized.
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  {notifications.slice(0, 5).map((notification: any) => (
                    <div
                      key={notification.id}
                      className="flex items-start p-4 bg-blue-50 rounded-lg"
                    >
                      <div className="flex-shrink-0">
                        <CalendarIcon className="h-5 w-5 text-blue-400 mt-0.5" />
                      </div>
                      <div className="ml-3 flex-1">
                        <p className="text-sm text-gray-900">{notification.message}</p>
                        <p className="text-xs text-gray-500 mt-1">
                          {formatDate(notification.notifyAt)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}