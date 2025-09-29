/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react/no-unescaped-entities */
// src/app/jobs/saved/page.tsx
'use client';

import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { SavedJob } from '@/lib/types';
import { formatDate, timeAgo } from '@/lib/utils';
import toast from 'react-hot-toast';
import Link from 'next/link';
import {
  BookmarkIcon,
  TrashIcon,
  ArrowTopRightOnSquareIcon,
  MagnifyingGlassIcon,
  MapPinIcon,
  BriefcaseIcon,
  CalendarIcon,
  PencilIcon,
} from '@heroicons/react/24/outline';

export default function SavedJobsPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch saved jobs
  const { data: savedJobs = [], isLoading, error } = useQuery({
    queryKey: ['saved-jobs'],
    queryFn: () => apiClient.getSavedJobs(),
    enabled: isAuthenticated,
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

  // Filter saved jobs
  const filteredJobs = savedJobs.filter((savedJob: SavedJob) => {
    if (!searchQuery) return true;
    
    // Note: In a real implementation, you'd want to include job details
    // For now, we'll search by notes
    return savedJob.notes?.toLowerCase().includes(searchQuery.toLowerCase());
  });

  const handleUnsaveJob = (savedJobId: string) => {
    if (window.confirm('Are you sure you want to remove this job from your saved list?')) {
      unsaveJobMutation.mutate(savedJobId);
    }
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Saved Jobs</h1>
              <p className="mt-1 text-gray-600">
                Jobs you've bookmarked for later review
              </p>
            </div>
            <Link
              href="/jobs"
              className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            >
              <MagnifyingGlassIcon className="-ml-1 mr-2 h-5 w-5" />
              Find More Jobs
            </Link>
          </div>
        </div>

        {/* Search */}
        <div className="px-4 py-4 sm:px-0">
          <div className="relative">
            <MagnifyingGlassIcon className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search saved jobs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
        </div>

        {/* Saved Jobs List */}
        <div className="bg-white shadow rounded-lg">
          {isLoading ? (
            <div className="animate-pulse p-6">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="flex items-start space-x-4 py-4 border-b border-gray-200 last:border-b-0">
                  <div className="h-12 w-12 bg-gray-200 rounded"></div>
                  <div className="flex-1">
                    <div className="h-5 bg-gray-200 rounded w-3/4 mb-2"></div>
                    <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
                    <div className="h-3 bg-gray-200 rounded w-1/3"></div>
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="text-center py-12">
              <div className="text-red-500 mb-2">Error loading saved jobs</div>
              <p className="text-gray-500">Please try refreshing the page</p>
            </div>
          ) : filteredJobs.length === 0 ? (
            <div className="text-center py-12">
              {savedJobs.length === 0 ? (
                <>
                  <BookmarkIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No saved jobs</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    Start by saving jobs you're interested in from the job search page.
                  </p>
                  <div className="mt-6">
                    <Link
                      href="/jobs"
                      className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                    >
                      <MagnifyingGlassIcon className="-ml-1 mr-2 h-5 w-5" />
                      Search Jobs
                    </Link>
                  </div>
                </>
              ) : (
                <>
                  <MagnifyingGlassIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">No matching saved jobs</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    Try adjusting your search criteria.
                  </p>
                </>
              )}
            </div>
          ) : (
            <ul className="divide-y divide-gray-200">
              {filteredJobs.map((savedJob: SavedJob) => (
                <li key={savedJob.id} className="px-6 py-4 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="text-lg font-medium text-gray-900">
                          Saved Job #{savedJob.id}
                        </h3>
                        <span className="text-sm text-gray-500">
                          Saved {timeAgo(savedJob.savedDate)}
                        </span>
                      </div>
                      
                      {savedJob.notes && (
                        <p className="text-sm text-gray-600 mb-2">
                          Notes: {savedJob.notes}
                        </p>
                      )}
                      
                      <div className="flex items-center text-xs text-gray-400 space-x-4">
                        <span>Job ID: {savedJob.jobListingId}</span>
                        <span>Saved: {formatDate(savedJob.savedDate)}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2 ml-4">
                      <Link
                        href={`/applications/new?jobId=${savedJob.jobListingId}`}
                        className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                      >
                        Apply Now
                      </Link>
                      <button
                        onClick={() => handleUnsaveJob(savedJob.id)}
                        disabled={unsaveJobMutation.isPending}
                        className="inline-flex items-center p-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-red-600 bg-white hover:bg-red-50 disabled:opacity-50"
                      >
                        <TrashIcon className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Results count */}
        {!isLoading && !error && filteredJobs.length > 0 && (
          <div className="mt-4 text-sm text-gray-500 text-center">
            Showing {filteredJobs.length} of {savedJobs.length} saved jobs
          </div>
        )}
      </div>
    </div>
  );
}
