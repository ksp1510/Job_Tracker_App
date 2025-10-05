/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react/no-unescaped-entities */
// src/app/jobs/[id]/page.tsx
'use client';

import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { useParams, useRouter } from 'next/navigation';
import { formatDate, timeAgo, formatSalary } from '@/lib/utils';
import toast from 'react-hot-toast';
import Link from 'next/link';
import {
  ArrowLeftIcon,
  BuildingOfficeIcon,
  MapPinIcon,
  CurrencyDollarIcon,
  ClockIcon,
  BriefcaseIcon,
  AcademicCapIcon,
  BookmarkIcon,
  CheckCircleIcon,
  XMarkIcon,
  PaperAirplaneIcon,
  ArrowTopRightOnSquareIcon,
} from '@heroicons/react/24/outline';
import { BookmarkIcon as BookmarkIconSolid } from '@heroicons/react/24/solid';
import { useForm } from 'react-hook-form';

interface QuickApplicationForm {
  coverLetter: string;
  portfolio?: string;
  availableStartDate?: string;
  expectedSalary?: string;
}

export default function JobDetailPage() {
  const { isAuthenticated } = useAuth();
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const jobId = params.id as string;
  const [showApplyModal, setShowApplyModal] = useState(false);

  // Fetch job details
  const { data: job, isLoading, error } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => apiClient.getJob(jobId),
    enabled: isAuthenticated && !!jobId,
  });

  // Fetch saved jobs to check if this job is saved
  const { data: savedJobs = [] } = useQuery({
    queryKey: ['saved-jobs'],
    queryFn: () => apiClient.getSavedJobs(),
    enabled: isAuthenticated,
  });

  // Form for quick application
  const { register, handleSubmit, formState: { errors }, reset } = useForm<QuickApplicationForm>();

  // Check if job is saved
  const savedJob = savedJobs.find((saved: any) => saved.jobListingId === jobId);
  const isJobSaved = !!savedJob;

  // Save/Unsave job mutation
  const saveJobMutation = useMutation({
    mutationFn: () => apiClient.saveJob(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job saved successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to save job');
    },
  });

  const unsaveJobMutation = useMutation({
    mutationFn: (id: string) => apiClient.unsaveJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job removed from saved');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to remove job');
    },
  });

  const handleSaveToggle = () => {
    if (isJobSaved) {
      unsaveJobMutation.mutate(savedJob.id);
    } else {
      saveJobMutation.mutate();
    }
  };

  const handleTrackApplication = () => {
    router.push(`/applications/new?jobId=${jobId}`);
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-5xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
            <div className="bg-white shadow rounded-lg p-8">
              <div className="h-10 bg-gray-200 rounded w-3/4 mb-4"></div>
              <div className="h-6 bg-gray-200 rounded w-1/2 mb-8"></div>
              <div className="grid grid-cols-2 gap-6 mb-8">
                {[...Array(6)].map((_, i) => (
                  <div key={i}>
                    <div className="h-4 bg-gray-200 rounded w-1/3 mb-2"></div>
                    <div className="h-6 bg-gray-200 rounded w-2/3"></div>
                  </div>
                ))}
              </div>
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="h-4 bg-gray-200 rounded"></div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-5xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="text-center py-12">
            <h3 className="text-lg font-medium text-gray-900">Job not found</h3>
            <p className="mt-1 text-sm text-gray-500">
              The job you're looking for doesn't exist or is no longer available.
            </p>
            <div className="mt-6">
              <Link
                href="/jobs"
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
              >
                <ArrowLeftIcon className="-ml-1 mr-2 h-5 w-5" />
                Back to Job Search
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
      
      <div className="max-w-5xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Back Button */}
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="inline-flex items-center text-sm font-medium text-gray-500 hover:text-gray-700"
          >
            <ArrowLeftIcon className="-ml-1 mr-1 h-5 w-5" />
            Back to Search
          </button>
        </div>

        {/* Job Header */}
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <div className="p-8">
            {/* Title and Actions */}
            <div className="flex items-start justify-between mb-6">
              <div className="flex-1">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                  {job.title}
                </h1>
                <div className="flex items-center space-x-4 text-lg text-gray-600">
                  <div className="flex items-center">
                    <BuildingOfficeIcon className="h-5 w-5 mr-2" />
                    {job.company}
                  </div>
                  {job.location && (
                    <div className="flex items-center">
                      <MapPinIcon className="h-5 w-5 mr-2" />
                      {job.location}
                    </div>
                  )}
                </div>
              </div>
              
              {/* Save Button */}
              <button
                onClick={handleSaveToggle}
                disabled={saveJobMutation.isPending || unsaveJobMutation.isPending}
                className="p-3 text-gray-400 hover:text-indigo-600 transition-colors disabled:opacity-50"
                title={isJobSaved ? 'Remove from saved' : 'Save job'}
              >
                {isJobSaved ? (
                  <BookmarkIconSolid className="h-7 w-7 text-indigo-600" />
                ) : (
                  <BookmarkIcon className="h-7 w-7" />
                )}
              </button>
            </div>

            {/* Job Meta Information */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6 py-6 border-t border-b border-gray-200">
              {job.jobType && (
                <div>
                  <dt className="text-sm font-medium text-gray-500 flex items-center mb-1">
                    <BriefcaseIcon className="h-4 w-4 mr-1" />
                    Job Type
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900">{job.jobType}</dd>
                </div>
              )}
              
              {job.experienceLevel && (
                <div>
                  <dt className="text-sm font-medium text-gray-500 flex items-center mb-1">
                    <AcademicCapIcon className="h-4 w-4 mr-1" />
                    Experience
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900">{job.experienceLevel}</dd>
                </div>
              )}
              
              {(job.salary || job.salaryRange) && (
                <div>
                  <dt className="text-sm font-medium text-gray-500 flex items-center mb-1">
                    <CurrencyDollarIcon className="h-4 w-4 mr-1" />
                    Salary
                  </dt>
                  <dd className="text-lg font-semibold text-green-600">
                    {job.salary && !isNaN(Number(job.salary))
                      ? formatSalary(Number(job.salary))
                      : job.salary || job.salaryRange}
                  </dd>
                </div>
              )}
              
              {job.postedDate && (
                <div>
                  <dt className="text-sm font-medium text-gray-500 flex items-center mb-1">
                    <ClockIcon className="h-4 w-4 mr-1" />
                    Posted
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900">{timeAgo(job.postedDate)}</dd>
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="mt-8 flex flex-wrap gap-3">
              
              {job.applyUrl ? (
                <a
                  href={job.applyUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex-1 sm:flex-none inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm"
                >
                  <ArrowTopRightOnSquareIcon className="-ml-1 mr-2 h-5 w-5" />
                  Apply on Company Site
                </a>
              ) : (
                <><button
                    className="btn-disabled"
                    disabled
                    title="No direct application link available"
                  >
                    ❌ No Apply Link Available
                  </button><a
                    href={`https://www.google.com/search?q=${encodeURIComponent(job.company || '')}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex-1 sm:flex-none inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm"
                    title="Search for this job on Google"
                  >
                      🔍 Search Company
                    </a></>
            )}
              
              <button
                onClick={handleTrackApplication}
                className="flex-1 sm:flex-none inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm"
              >
                Mark as Applied
              </button>
            </div>
          </div>
        </div>

        {/* Job Description */}
        <div className="mt-6 bg-white shadow rounded-lg p-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Job Description</h2>
          <div className="prose max-w-none">
            <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">
              {job.description}
            </p>
          </div>
        </div>

        {/* Skills Section */}
        {job.skills && job.skills.length > 0 && (
          <div className="mt-6 bg-white shadow rounded-lg p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Required Skills</h2>
            <div className="flex flex-wrap gap-2">
              {job.skills.map((skill, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-4 py-2 rounded-md text-sm font-medium bg-indigo-100 text-indigo-800"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Additional Information */}
        <div className="mt-6 bg-white shadow rounded-lg p-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Additional Information</h2>
          <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <dt className="text-sm font-medium text-gray-500">Source</dt>
              <dd className="mt-1 text-sm text-gray-900">{job.source}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">Posted Date</dt>
              <dd className="mt-1 text-sm text-gray-900">{formatDate(job.postedDate)}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">Status</dt>
              <dd className="mt-1">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  job.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                }`}>
                  {job.active ? 'Active' : 'Inactive'}
                </span>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </div>
  );
}