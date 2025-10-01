/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react/no-unescaped-entities */
// src/app/applications/[id]/page.tsx
'use client';

import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { useParams, useRouter } from 'next/navigation';
import { formatDate, formatDateTime, getStatusColor } from '@/lib/utils';
import toast from 'react-hot-toast';
import Link from 'next/link';
import {
  PencilIcon,
  TrashIcon,
  ArrowLeftIcon,
  BuildingOfficeIcon,
  MapPinIcon,
  CurrencyDollarIcon,
  LinkIcon,
  UserIcon,
  CalendarIcon,
  DocumentTextIcon,
} from '@heroicons/react/24/outline';

export default function ApplicationDetailPage() {
  const { isAuthenticated } = useAuth();
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const applicationId = params.id as string;

  // Fetch application details
  const { data: application, isLoading, error } = useQuery({
    queryKey: ['application', applicationId],
    queryFn: () => apiClient.getApplication(applicationId),
    enabled: isAuthenticated && !!applicationId,
  });

  // Delete application mutation
  const deleteApplicationMutation = useMutation({
    mutationFn: () => apiClient.deleteApplication(applicationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      toast.success('Application deleted successfully');
      router.push('/applications');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to delete application');
    },
  });

  const handleDelete = () => {
    if (window.confirm(`Are you sure you want to delete the application for ${application?.jobTitle} at ${application?.companyName}?`)) {
      deleteApplicationMutation.mutate();
    }
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-4xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/3 mb-4"></div>
            <div className="bg-white shadow rounded-lg p-6">
              <div className="grid grid-cols-2 gap-6">
                {[...Array(8)].map((_, i) => (
                  <div key={i}>
                    <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                    <div className="h-6 bg-gray-200 rounded w-3/4"></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !application) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-4xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="text-center">
            <h3 className="text-lg font-medium text-gray-900">Application not found</h3>
            <p className="mt-1 text-sm text-gray-500">
              The application you're looking for doesn't exist or you don't have permission to view it.
            </p>
            <div className="mt-6">
              <Link
                href="/applications"
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
              >
                <ArrowLeftIcon className="-ml-1 mr-2 h-5 w-5" />
                Back to Applications
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-4xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => router.back()}
                className="inline-flex items-center text-sm font-medium text-gray-500 hover:text-gray-700"
              >
                <ArrowLeftIcon className="-ml-1 mr-1 h-5 w-5" />
                Back
              </button>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">
                  {application.jobTitle}
                </h1>
                <p className="mt-1 text-lg text-gray-600">
                  {application.companyName}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-3">
              <Link
                href={`/applications/${application.id}/edit`}
                className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                <PencilIcon className="-ml-1 mr-2 h-5 w-5" />
                Edit
              </Link>
              <button
                onClick={handleDelete}
                disabled={deleteApplicationMutation.isPending}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 disabled:opacity-50"
              >
                <TrashIcon className="-ml-1 mr-2 h-5 w-5" />
                {deleteApplicationMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>

        {/* Application Details */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            {/* Status Badge */}
            <div className="mb-6">
              <span
                className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(
                  application.status
                )}`}
              >
                {application.status}
              </span>
            </div>

            {/* Details Grid */}
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              {/* Company */}
              <div className="flex items-center space-x-3">
                <BuildingOfficeIcon className="h-5 w-5 text-gray-700" />
                <div>
                  <dt className="text-sm font-medium text-gray-500">Company</dt>
                  <dd className="text-lg text-gray-900">{application.companyName}</dd>
                </div>
              </div>

              {/* Location */}
              {application.jobLocation && (
                <div className="flex items-center space-x-3">
                  <MapPinIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Location</dt>
                    <dd className="text-lg text-gray-900">{application.jobLocation}</dd>
                  </div>
                </div>
              )}

              {/* Salary */}
              {application.salary && (
                <div className="flex items-center space-x-3">
                  <CurrencyDollarIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Salary</dt>
                    <dd className="text-lg text-gray-900">{application.salary}</dd>
                  </div>
                </div>
              )}

              {/* Applied Date */}
              {application.appliedDate && (
                <div className="flex items-center space-x-3">
                  <CalendarIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Applied Date</dt>
                    <dd className="text-lg text-gray-900">{formatDate(application.appliedDate)}</dd>
                  </div>
                </div>
              )}

              {/* Interview Date */}
              {application.interviewDate && (
                <div className="flex items-center space-x-3">
                  <CalendarIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Interview Date</dt>
                    <dd className="text-lg text-gray-900">{formatDateTime(application.interviewDate)}</dd>
                  </div>
                </div>
              )}

              {/* Assessment Date */}
              {application.assessmentDate && (
                <div className="flex items-center space-x-3">
                  <CalendarIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Assessment Date</dt>
                    <dd className="text-lg text-gray-900">{formatDateTime(application.assessmentDate)}</dd>
                  </div>
                </div>
              )}

              {/* Recruiter Contact */}
              {application.recruiterContact && (
                <div className="flex items-center space-x-3">
                  <UserIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Recruiter Contact</dt>
                    <dd className="text-lg text-gray-900">{application.recruiterContact}</dd>
                  </div>
                </div>
              )}

              {/* Referral */}
              {application.referral && (
                <div className="flex items-center space-x-3">
                  <UserIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Referral</dt>
                    <dd className="text-lg text-gray-900">{application.referral}</dd>
                  </div>
                </div>
              )}

              {/* Job Link */}
              {application.jobLink && (
                <div className="flex items-center space-x-3">
                  <LinkIcon className="h-5 w-5 text-gray-700" />
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Job Link</dt>
                    <dd className="text-lg text-gray-900">
                      <a
                        href={application.jobLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-indigo-600 hover:text-indigo-500"
                      >
                        View Job Posting
                      </a>
                    </dd>
                  </div>
                </div>
              )}
            </div>

            {/* Job Description */}
            {application.jobDescription && (
              <div className="mt-8">
                <div className="flex items-center space-x-2 mb-3">
                  <DocumentTextIcon className="h-5 w-5 text-gray-700" />
                  <h3 className="text-lg font-medium text-gray-900">Job Description</h3>
                </div>
                <div className="prose max-w-none">
                  <p className="text-gray-700 whitespace-pre-wrap">{application.jobDescription}</p>
                </div>
              </div>
            )}

            {/* Notes */}
            {application.notes && (
              <div className="mt-8">
                <div className="flex items-center space-x-2 mb-3">
                  <DocumentTextIcon className="h-5 w-5 text-gray-700" />
                  <h3 className="text-lg font-medium text-gray-900">Notes</h3>
                </div>
                <div className="prose max-w-none">
                  <p className="text-gray-700 whitespace-pre-wrap">{application.notes}</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-8 bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h3>
            <div className="flex flex-wrap gap-3">
              <Link
                href={`/notifications?applicationId=${application.id}`}
                className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                <CalendarIcon className="-ml-1 mr-2 h-5 w-5" />
                Set Reminder
              </Link>
              <Link
                href={`/applications/${application.id}/edit`}
                className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                <PencilIcon className="-ml-1 mr-2 h-5 w-5" />
                Update Status
              </Link>
              {application.jobLink && (
                <a
                  href={application.jobLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                >
                  <LinkIcon className="-ml-1 mr-2 h-5 w-5" />
                  View Original Posting
                </a>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}