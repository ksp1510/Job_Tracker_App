/* eslint-disable react/no-unescaped-entities */
// src/app/dashboard/page.tsx
'use client';

import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Application, ApplicationStatus } from '@/lib/types';
import { formatDate, getStatusColor } from '@/lib/utils';
import Link from 'next/link';
import {
  PlusIcon,
  DocumentDuplicateIcon,
  BellIcon,
  MagnifyingGlassIcon,
  CalendarIcon,
  ClockIcon,
  ArrowTrendingUpIcon,
} from '@heroicons/react/24/outline';

export default function DashboardPage() {
  const { user, isAuthenticated } = useAuth();

  // Fetch recent applications
  const { data: applications = [], isLoading } = useQuery({
    queryKey: ['applications'],
    queryFn: () => apiClient.getApplications(),
    enabled: isAuthenticated,
  });

  // Fetch unread notifications
  const { data: unreadNotifications = [] } = useQuery({
    queryKey: ['unread-notifications'],
    queryFn: () => apiClient.getUnreadNotifications(),
    enabled: isAuthenticated,
  });

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  // Calculate stats
  const totalApplications = applications.length;
  const pendingApplications = applications.filter(app => 
    app.status === ApplicationStatus.APPLIED || app.status === ApplicationStatus.INTERVIEW
  ).length;
  const interviews = applications.filter(app => app.status === ApplicationStatus.INTERVIEW).length;
  const offers = applications.filter(app => app.status === ApplicationStatus.OFFER).length;

  // Recent applications (last 5)
  const recentApplications = applications
    .sort((a, b) => new Date(b.appliedDate || '').getTime() - new Date(a.appliedDate || '').getTime())
    .slice(0, 5);

  // Upcoming interviews
  const upcomingInterviews = applications
    .filter(app => app.interviewDate && new Date(app.interviewDate) > new Date())
    .sort((a, b) => new Date(a.interviewDate!).getTime() - new Date(b.interviewDate!).getTime())
    .slice(0, 3);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Welcome Header */}
        <div className="px-4 py-6 sm:px-0">
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="mt-1 text-lg text-gray-600">
            Here's an overview of your job search progress
          </p>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <Link
            href="/jobs"
            className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg shadow hover:shadow-md transition-shadow"
          >
            <div>
              <span className="rounded-lg inline-flex p-3 bg-blue-50 text-blue-600 group-hover:bg-blue-100">
                <MagnifyingGlassIcon className="h-6 w-6" />
              </span>
            </div>
            <div className="mt-4">
              <h3 className="text-lg font-medium text-gray-900">Search Jobs</h3>
              <p className="mt-2 text-sm text-gray-500">
                Find new opportunities
              </p>
            </div>
          </Link>
          
          <Link
            href="/applications/new"
            className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg shadow hover:shadow-md transition-shadow"
          >
            <div>
              <span className="rounded-lg inline-flex p-3 bg-indigo-50 text-indigo-600 group-hover:bg-indigo-100">
                <PlusIcon className="h-6 w-6" />
              </span>
            </div>
            <div className="mt-4">
              <h3 className="text-lg font-medium text-gray-900">Add Application</h3>
              <p className="mt-2 text-sm text-gray-500">
                Track a new job application
              </p>
            </div>
          </Link>

          <Link
            href="/applications"
            className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg shadow hover:shadow-md transition-shadow"
          >
            <div>
              <span className="rounded-lg inline-flex p-3 bg-green-50 text-green-600 group-hover:bg-green-100">
                <DocumentDuplicateIcon className="h-6 w-6" />
              </span>
            </div>
            <div className="mt-4">
              <h3 className="text-lg font-medium text-gray-900">View Applications</h3>
              <p className="mt-2 text-sm text-gray-500">
                Manage your applications
              </p>
            </div>
          </Link>

          <Link
            href="/notifications"
            className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg shadow hover:shadow-md transition-shadow"
          >
            <div>
              <span className="rounded-lg inline-flex p-3 bg-yellow-50 text-yellow-600 group-hover:bg-yellow-100">
                <BellIcon className="h-6 w-6" />
              </span>
              {unreadNotifications.length > 0 && (
                <span className="flex top-2 right-2 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                  {unreadNotifications.length}
                </span>
              )}
            </div>
            <div className="mt-4">
              <h3 className="text-lg font-medium text-gray-900">Notifications</h3>
              <p className="mt-2 text-sm text-gray-500">
                Check reminders & updates
              </p>
            </div>
          </Link>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <DocumentDuplicateIcon className="h-8 w-8 text-gray-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total Applications
                    </dt>
                    <dd className="text-3xl font-semibold text-gray-900">
                      {totalApplications}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <ClockIcon className="h-8 w-8 text-yellow-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Pending
                    </dt>
                    <dd className="text-3xl font-semibold text-yellow-600">
                      {pendingApplications}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <CalendarIcon className="h-8 w-8 text-blue-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Interviews
                    </dt>
                    <dd className="text-3xl font-semibold text-blue-600">
                      {interviews}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <ArrowTrendingUpIcon className="h-8 w-8 text-green-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Offers
                    </dt>
                    <dd className="text-3xl font-semibold text-green-600">
                      {offers}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
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
                      <div className="h-10 w-10 bg-gray-200 rounded-full"></div>
                      <div className="flex-1">
                        <div className="h-4 bg-gray-200 rounded w-3/4 mb-1"></div>
                        <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : recentApplications.length === 0 ? (
                <div className="text-center py-6">
                  <DocumentDuplicateIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No applications yet</h3>
                  <p className="mt-1 text-sm text-gray-500">Get started by adding your first application.</p>
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
                <div className="flow-root">
                  <ul className="-my-5 divide-y divide-gray-200">
                    {recentApplications.map((application) => (
                      <li key={application.id} className="py-4">
                        <div className="flex items-center space-x-4">
                          <div className="flex-shrink-0">
                            <div className="h-8 w-8 rounded-full bg-gray-200 flex items-center justify-center">
                              <span className="text-sm font-medium text-gray-600">
                                {application.companyName.charAt(0)}
                              </span>
                            </div>
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-gray-900 truncate">
                              {application.jobTitle}
                            </p>
                            <p className="text-sm text-gray-500 truncate">
                              {application.companyName}
                            </p>
                          </div>
                          <div className="flex items-center space-x-2">
                            <span
                              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(
                                application.status
                              )}`}
                            >
                              {application.status}
                            </span>
                            <p className="text-xs text-gray-500">
                              {application.appliedDate && formatDate(application.appliedDate)}
                            </p>
                          </div>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>

          {/* Upcoming Interviews */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Upcoming Interviews
                </h3>
                <Link
                  href="/notifications"
                  className="text-sm font-medium text-indigo-600 hover:text-indigo-500"
                >
                  Set reminders
                </Link>
              </div>

              {upcomingInterviews.length === 0 ? (
                <div className="text-center py-6">
                  <CalendarIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No upcoming interviews</h3>
                  <p className="mt-1 text-sm text-gray-500">Your interview schedule will appear here.</p>
                </div>
              ) : (
                <div className="flow-root">
                  <ul className="-my-5 divide-y divide-gray-200">
                    {upcomingInterviews.map((application) => (
                      <li key={application.id} className="py-4">
                        <div className="flex items-center space-x-4">
                          <div className="flex-shrink-0">
                            <CalendarIcon className="h-6 w-6 text-blue-500" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-gray-900 truncate">
                              {application.jobTitle}
                            </p>
                            <p className="text-sm text-gray-500 truncate">
                              {application.companyName}
                            </p>
                            {application.interviewDate && (
                              <p className="text-xs text-gray-400">
                                {formatDate(application.interviewDate)} at{' '}
                                {new Date(application.interviewDate).toLocaleTimeString([], {
                                  hour: '2-digit',
                                  minute: '2-digit'
                                })}
                              </p>
                            )}
                          </div>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Recent Notifications */}
        {unreadNotifications.length > 0 && (
          <div className="mt-8 bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Recent Notifications
                </h3>
                <Link
                  href="/notifications"
                  className="text-sm font-medium text-indigo-600 hover:text-indigo-500"
                >
                  View all ({unreadNotifications.length})
                </Link>
              </div>
              
              <div className="flow-root">
                <ul className="-my-5 divide-y divide-gray-200">
                  {unreadNotifications.slice(0, 3).map((notification) => (
                    <li key={notification.id} className="py-4">
                      <div className="flex items-center space-x-4">
                        <div className="flex-shrink-0">
                          <BellIcon className="h-6 w-6 text-yellow-500" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-gray-900">
                            {notification.message}
                          </p>
                          <p className="text-xs text-gray-500">
                            {formatDate(notification.notifyAt)}
                          </p>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

