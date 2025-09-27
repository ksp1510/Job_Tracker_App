/* eslint-disable @typescript-eslint/no-explicit-any */
// src/app/jobs/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { JobListing, JobSearchParams, SavedJob } from '@/lib/types';
import { useForm } from 'react-hook-form';
import { debounce, formatSalary, timeAgo } from '@/lib/utils';
import toast from 'react-hot-toast';
import {
  MagnifyingGlassIcon,
  MapPinIcon,
  BookmarkIcon,
  ArrowTopRightOnSquareIcon,
  BriefcaseIcon,
  CurrencyDollarIcon,
  CalendarIcon,
} from '@heroicons/react/24/outline';
import { BookmarkIcon as BookmarkIconSolid } from '@heroicons/react/24/solid';
import Link from 'next/link';

export default function JobSearchPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useState<JobSearchParams>({
    page: 0,
    size: 10,
  });

  const { register, handleSubmit, watch } = useForm<{
    query: string;
    location: string;
    jobType: string;
    minSalary: string;
    maxSalary: string;
  }>();

  // Watch form values for real-time search
  const watchedValues = watch();

  // Debounced search effect
  useEffect(() => {
    const debouncedSearch = debounce(() => {
      const newParams: JobSearchParams = {
        ...searchParams,
        query: watchedValues.query || undefined,
        location: watchedValues.location || undefined,
        jobType: watchedValues.jobType || undefined,
        minSalary: watchedValues.minSalary ? parseFloat(watchedValues.minSalary) : undefined,
        maxSalary: watchedValues.maxSalary ? parseFloat(watchedValues.maxSalary) : undefined,
        page: 0, // Reset to first page on search
      };
      setSearchParams(newParams);
    }, 500);

    debouncedSearch();
  }, [watchedValues]);

  // Fetch jobs
  const { data: jobsResponse, isLoading, error } = useQuery({
    queryKey: ['jobs', searchParams],
    queryFn: () => apiClient.searchJobs(searchParams),
    enabled: isAuthenticated,
    // v5 replacement for keepPreviousData
    placeholderData: (prev) => prev,
  });

  // Fetch saved jobs to mark as saved
  const { data: savedJobs = [] as SavedJob[] } = useQuery<SavedJob[]>({
    queryKey: ['saved-jobs'],
    queryFn: () => apiClient.getSavedJobs(),
    enabled: isAuthenticated,
  });

  // Save job mutation
  const saveJobMutation = useMutation({
    mutationFn: ({ jobId, notes }: { jobId: string; notes?: string }) =>
      apiClient.saveJob(jobId, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job saved successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to save job');
    },
  });

  // Unsave job mutation
  const unsaveJobMutation = useMutation({
    mutationFn: (savedJobId: string) => apiClient.unsaveJob(savedJobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job removed from saved');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to remove saved job');
    },
  });

  const handleSaveJob = (job: JobListing) => {
    const savedJob = savedJobs.find((saved: SavedJob) => saved.jobListingId === job.id);
    if (savedJob) {
      unsaveJobMutation.mutate(savedJob.id);
    } else {
      saveJobMutation.mutate({ jobId: job.id });
    }
  };

  const isJobSaved = (jobId: string) => {
    return savedJobs.some((saved: SavedJob) => saved.jobListingId === jobId);
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams(prev => ({ ...prev, page: newPage }));
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  const jobs = jobsResponse?.content || [];
  const totalPages = jobsResponse?.totalPages || 0;
  const currentPage = jobsResponse?.page || 0;

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <h1 className="text-2xl font-bold text-gray-900">Job Search</h1>
          <p className="mt-1 text-gray-600">
            Find your next opportunity
          </p>
        </div>

        {/* Search Filters */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="p-6">
            <form className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
              {/* Job Title/Keywords */}
              <div>
                <label htmlFor="query" className="block text-sm font-medium text-gray-700">
                  Job Title / Keywords
                </label>
                <div className="mt-1 relative">
                  <MagnifyingGlassIcon className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                  <input
                    {...register('query')}
                    type="text"
                    placeholder="e.g. Software Engineer"
                    className="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              {/* Location */}
              <div>
                <label htmlFor="location" className="block text-sm font-medium text-gray-700">
                  Location
                </label>
                <div className="mt-1 relative">
                  <MapPinIcon className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                  <input
                    {...register('location')}
                    type="text"
                    placeholder="e.g. San Francisco, CA"
                    className="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              {/* Job Type */}
              <div>
                <label htmlFor="jobType" className="block text-sm font-medium text-gray-700">
                  Job Type
                </label>
                <select
                  {...register('jobType')}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                >
                  <option value="">All Types</option>
                  <option value="FULL_TIME">Full Time</option>
                  <option value="PART_TIME">Part Time</option>
                  <option value="CONTRACT">Contract</option>
                  <option value="INTERNSHIP">Internship</option>
                  <option value="REMOTE">Remote</option>
                </select>
              </div>

              {/* Min Salary */}
              <div>
                <label htmlFor="minSalary" className="block text-sm font-medium text-gray-700">
                  Min Salary
                </label>
                <div className="mt-1 relative">
                  <CurrencyDollarIcon className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                  <input
                    {...register('minSalary')}
                    type="number"
                    placeholder="50000"
                    className="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              {/* Max Salary */}
              <div>
                <label htmlFor="maxSalary" className="block text-sm font-medium text-gray-700">
                  Max Salary
                </label>
                <div className="mt-1 relative">
                  <CurrencyDollarIcon className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                  <input
                    {...register('maxSalary')}
                    type="number"
                    placeholder="150000"
                    className="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            </form>
          </div>
        </div>

        {/* Results */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            {/* Results Header */}
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-medium text-gray-900">
                  Job Results
                </h3>
                {jobsResponse && (
                  <p className="text-sm text-gray-500">
                    {jobsResponse.totalElements} jobs found
                  </p>
                )}
              </div>
              <Link
                href="/jobs/saved"
                className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                <BookmarkIcon className="w-4 h-4 mr-2" />
                Saved Jobs ({savedJobs.length})
              </Link>
            </div>

            {/* Loading State */}
            {isLoading && (
              <div className="animate-pulse space-y-4">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="border border-gray-200 rounded-lg p-6">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="h-5 bg-gray-200 rounded w-3/4 mb-2"></div>
                        <div className="h-4 bg-gray-200 rounded w-1/2 mb-4"></div>
                        <div className="h-3 bg-gray-200 rounded w-full mb-2"></div>
                        <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                      </div>
                      <div className="h-8 w-8 bg-gray-200 rounded"></div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Error State */}
            {error && (
              <div className="text-center py-12">
                <div className="text-red-500 mb-2">
                  <MagnifyingGlassIcon className="mx-auto h-12 w-12" />
                </div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  Search Error
                </h3>
                <p className="text-gray-500">
                  There was an error searching for jobs. Please try again.
                </p>
              </div>
            )}

            {/* Empty State */}
            {!isLoading && !error && jobs.length === 0 && (
              <div className="text-center py-12">
                <MagnifyingGlassIcon className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-lg font-medium text-gray-900">No jobs found</h3>
                <p className="mt-1 text-gray-500">
                  Try adjusting your search criteria to find more opportunities.
                </p>
              </div>
            )}

            {/* Job Results */}
            {!isLoading && !error && jobs.length > 0 && (
              <div className="space-y-4">
                {jobs.map((job: JobListing) => (
                  <div
                    key={job.id}
                    className="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-2">
                          <h3 className="text-lg font-medium text-gray-900 truncate">
                            {job.title}
                          </h3>
                          <button
                            onClick={() => handleSaveJob(job)}
                            disabled={saveJobMutation.isPending || unsaveJobMutation.isPending}
                            className="ml-4 p-2 text-gray-400 hover:text-indigo-600 transition-colors"
                          >
                            {isJobSaved(job.id) ? (
                              <BookmarkIconSolid className="h-5 w-5 text-indigo-600" />
                            ) : (
                              <BookmarkIcon className="h-5 w-5" />
                            )}
                          </button>
                        </div>
                        
                        <div className="flex items-center text-sm text-gray-500 mb-3">
                          <BriefcaseIcon className="h-4 w-4 mr-1" />
                          <span className="mr-4">{job.company}</span>
                          {job.location && (
                            <>
                              <MapPinIcon className="h-4 w-4 mr-1" />
                              <span className="mr-4">{job.location}</span>
                            </>
                          )}
                          {job.postedDate && (
                            <>
                              <CalendarIcon className="h-4 w-4 mr-1" />
                              <span>{timeAgo(job.postedDate)}</span>
                            </>
                          )}
                        </div>

                        <p className="text-sm text-gray-700 mb-4 line-clamp-3">
                          {job.description}
                        </p>

                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-4 text-sm">
                            {job.jobType && (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                {job.jobType}
                              </span>
                            )}
                            {job.experienceLevel && (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                                {job.experienceLevel}
                              </span>
                            )}
                            {job.salary && (
                              <span className="text-green-600 font-medium">
                                {!isNaN(Number(job.salary)) ? formatSalary(Number(job.salary)) : job.salary}
                              </span>
                            )}
                            {job.salaryRange && !job.salary && (
                              <span className="text-green-600 font-medium">
                                {job.salaryRange}
                              </span>
                            )}
                          </div>
                          
                          <div className="flex space-x-2">
                            {job.applyUrl && (
                              <a
                                href={job.applyUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                              >
                                <ArrowTopRightOnSquareIcon className="w-4 h-4 mr-1" />
                                Apply
                              </a>
                            )}
                            <Link
                              href={`/applications/new?jobId=${job.id}`}
                              className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                            >
                              Track Application
                            </Link>
                          </div>
                        </div>

                        {/* Skills */}
                        {job.skills && job.skills.length > 0 && (
                          <div className="mt-4">
                            <div className="flex flex-wrap gap-2">
                              {job.skills.slice(0, 5).map((skill, index) => (
                                <span
                                  key={index}
                                  className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-800"
                                >
                                  {skill}
                                </span>
                              ))}
                              {job.skills.length > 5 && (
                                <span className="text-xs text-gray-500">
                                  +{job.skills.length - 5} more
                                </span>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Pagination */}
            {!isLoading && !error && jobs.length > 0 && totalPages > 1 && (
              <div className="mt-6 flex items-center justify-between">
                <div className="text-sm text-gray-700">
                  Page {currentPage + 1} of {totalPages}
                </div>
                <div className="flex space-x-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}