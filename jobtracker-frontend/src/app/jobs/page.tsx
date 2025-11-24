/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable @typescript-eslint/no-explicit-any */
'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { JobListing, JobSearchParams, SavedJob } from '@/lib/types';
import { useForm } from 'react-hook-form';
import { formatSalary, timeAgo } from '@/lib/utils';
import { toast } from 'react-hot-toast';
import debounce from 'lodash.debounce';
import {
  MagnifyingGlassIcon,
  MapPinIcon,
  BookmarkIcon,
  ArrowTopRightOnSquareIcon,
  BriefcaseIcon,
  CurrencyDollarIcon,
  CalendarIcon,
  XMarkIcon,
  FunnelIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  AdjustmentsHorizontalIcon,
} from '@heroicons/react/24/outline';
import { BookmarkIcon as BookmarkIconSolid, CheckCircleIcon } from '@heroicons/react/24/solid';
import Link from 'next/link';

const JOB_TYPES = [
  { value: '', label: 'All Types' },
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'REMOTE', label: 'Remote' },
];

const ITEMS_PER_PAGE = 12;

export default function JobSearchPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const [showFilters, setShowFilters] = useState(true);
  const [appliedJobs, setAppliedJobs] = useState<Set<string>>(new Set());
  const [isSearching, setIsSearching] = useState(false);
  const [searchParams, setSearchParams] = useState<JobSearchParams>({ page: 0, size: ITEMS_PER_PAGE });
  const [hasSearched, setHasSearched] = useState(false);
  const [savedJobIds, setSavedJobIds] = useState<Set<string>>(new Set());
  
  const { register, watch, setValue } = useForm({
    defaultValues: { query: '', location: '', jobType: '', minSalary: '', maxSalary: '', skills: '' },
  });

  const watchedValues = watch();

  /** Load applied jobs from localStorage */
  useEffect(() => {
    try {
      const stored = localStorage.getItem('appliedJobs');
      if (stored) setAppliedJobs(new Set(JSON.parse(stored)));
    } catch {
      console.error('Failed to parse applied jobs');
    }
  }, []);

  /** Load last search from localStorage */
  useEffect(() => {
    const lastQuery = localStorage.getItem("lastQuery") || "";
    const lastLocation = localStorage.getItem("lastLocation") || "";
    
    if (lastQuery || lastLocation) {
      console.log("📦 Found previous search in localStorage");
      setHasSearched(true);
      setSearchParams({
        query: lastQuery || undefined,
        location: lastLocation || undefined,
        page: 0,
        size: ITEMS_PER_PAGE
      });
    }
  }, []);

  /** Fetch jobs with caching */
  const { data: jobsResponse, isLoading, error, isFetching } = useQuery({
    queryKey: ['jobs', searchParams],
    queryFn: async () => {
      const hasCached = await apiClient.checkCacheStatus();
      const lastQuery = localStorage.getItem("lastQuery") || "";
      const lastLocation = localStorage.getItem("lastLocation") || "";

      // Use cache if query and location match
      if (
        hasCached &&
        lastQuery === (searchParams.query?.trim() || "") &&
        lastLocation === (searchParams.location?.trim() || "")
      ) {
        console.log("✅ Using cached results");
        const cached = await apiClient.getCachedResults(
          searchParams.page ?? 0,
          searchParams.size ?? ITEMS_PER_PAGE,
          searchParams.query,
          searchParams.location
        );
        if (cached) return cached;
      }

      console.log("🔍 Fetching new results from backend");
      const fresh = await apiClient.searchJobs(searchParams);
      localStorage.setItem("lastQuery", searchParams.query?.trim() || "");
      localStorage.setItem("lastLocation", searchParams.location?.trim() || "");
      return fresh;
    },
    enabled: isAuthenticated && hasSearched,
  });

  /** Extract data from response */
  const jobs: JobListing[] = (jobsResponse as any)?.content || [];
  const totalElements = (jobsResponse as any)?.totalElements || 0;
  const totalPages = (jobsResponse as any)?.totalPages || 0;
  const currentPage = (jobsResponse as any)?.page ?? searchParams.page ?? 0;

  /** Fetch saved jobs */
  const { data: savedJobs = [] } = useQuery<SavedJob[]>({
    queryKey: ['saved-jobs'],
    queryFn: apiClient.getSavedJobs,
    enabled: isAuthenticated,
  });

  /** Sync savedJobIds with server data - FIXED to prevent infinite loop */
  useEffect(() => {
    if (!savedJobs) return;
    
    const newIds = new Set(savedJobs.map((s: SavedJob) => s.jobListingId));
    
    setSavedJobIds(prevIds => {
      // Only update if the IDs have actually changed
      if (prevIds.size !== newIds.size) return newIds;
      
      // Check if all IDs are the same
      for (const id of newIds) {
        if (!prevIds.has(id)) return newIds;
      }
      
      // No changes, return previous state to prevent re-render
      return prevIds;
    });
  }, [savedJobs]);

  /** Fetch applications */
  const { data: applications = [] } = useQuery({
    queryKey: ['applications'],
    queryFn: apiClient.getApplications,
    enabled: isAuthenticated,
  });

  /** Sync applied jobs */
  useEffect(() => {
    if (applications.length > 0) {
      const appliedSet = new Set<string>();
      for (const a of applications) {
        if (a.externalJobId) appliedSet.add(a.externalJobId);
      }
      try {
        const storedIds = JSON.parse(localStorage.getItem('appliedJobs') || '[]');
        storedIds.forEach((id: string) => appliedSet.add(id));
      } catch {}
      setAppliedJobs(appliedSet);
      localStorage.setItem('appliedJobs', JSON.stringify([...appliedSet]));
    }
  }, [applications]);

  /** Mutations */
  const saveJobMutation = useMutation({
    mutationFn: ({ jobId, notes }: { jobId: string; notes?: string }) => 
      apiClient.saveJob(jobId, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job saved');
    },
    onError: (error) => {
      console.error('Failed to save job:', error);
      toast.error('Failed to save job');
    }
  });

  const unsaveJobMutation = useMutation({
    mutationFn: apiClient.unsaveJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-jobs'] });
      toast.success('Job removed from saved');
    },
    onError: (error) => {
      console.error('Failed to unsave job:', error);
      toast.error('Failed to unsave job');
    }
  });

  const markAsAppliedMutation = useMutation({
    mutationFn: (job: JobListing) =>
      apiClient.createApplication({
        companyName: job.company,
        jobTitle: job.title,
        jobLocation: job.location,
        jobDescription: job.description,
        jobLink: job.applyUrl,
        status: 'APPLIED' as any,
        salary: (() => {
          const raw = job.salary || job.salaryRange;
          if (!raw) return undefined;
          const clean = String(raw).replace(/[^0-9.-]/g, "").trim();
          if (clean.includes("-")) {
            const [min, max] = clean.split("-").map(Number);
            return !isNaN(min) && !isNaN(max) ? (min + max) / 2 : undefined;
          }
          const num = Number(clean);
          return isNaN(num) ? undefined : num;
        })(),
        salaryText: job.salaryRange,
        appliedDate: new Date().toISOString(),
        notes: `Applied via CareerTrackr on ${new Date().toLocaleDateString()}`,
        externalJobId: job.id,
      }),
    onSuccess: (_, job) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      setAppliedJobs((prev) => {
        const set = new Set([...prev, job.id]);
        localStorage.setItem('appliedJobs', JSON.stringify([...set]));
        return set;
      });
      toast.success('Marked as applied');
    },
    onError: (error) => {
      console.error('Failed to mark as applied:', error);
      toast.error('Failed to mark as applied');
    }
  });

  /** Handlers */
  const handleSaveJob = async (jobId: string) => {
    try {
      if (isJobSaved(jobId)) {
        // Unsave the job
        const savedJob = savedJobs.find(s => s.jobListingId === jobId);
        if (savedJob) {
          await unsaveJobMutation.mutateAsync(savedJob.id);
          // Optimistically update UI
          setSavedJobIds(prev => {
            const newSet = new Set(prev);
            newSet.delete(jobId);
            return newSet;
          });
        }
      } else {
        // Save the job
        await saveJobMutation.mutateAsync({ jobId, notes: '' });
        // Optimistically update UI
        setSavedJobIds(prev => new Set([...prev, jobId]));
      }
    } catch (error) {
      console.error('Error in handleSaveJob:', error);
    }
  };

  const handleMarkAsApplied = (job: JobListing) => {
    if (window.confirm(`Mark "${job.title}" at ${job.company} as applied?`)) {
      markAsAppliedMutation.mutate(job);
    }
  };

  const debouncedSearch = useCallback(
    debounce((params: JobSearchParams) => {
      setSearchParams(params);
      setIsSearching(false);
    }, 500),
    []
  );

  useEffect(() => () => debouncedSearch.cancel(), [debouncedSearch]);

  const handleSearch = async () => {
    if (
      !watchedValues.query?.trim() &&
      !watchedValues.location?.trim() &&
      !watchedValues.jobType &&
      !watchedValues.minSalary &&
      !watchedValues.maxSalary &&
      !watchedValues.skills
    ) {
      toast.error('Please enter at least one search criteria');
      return;
    }

    setIsSearching(true);
    setHasSearched(true);

    try {
      const skillsArray = watchedValues.skills
        ? watchedValues.skills.split(',').map((s) => s.trim()).filter(Boolean)
        : undefined;
      
      const params: JobSearchParams = {
        query: watchedValues.query?.trim() || undefined,
        location: watchedValues.location?.trim() || undefined,
        jobType: watchedValues.jobType || undefined,
        minSalary: watchedValues.minSalary ? parseFloat(watchedValues.minSalary) : undefined,
        maxSalary: watchedValues.maxSalary ? parseFloat(watchedValues.maxSalary) : undefined,
        skills: skillsArray,
        page: 0,
        size: ITEMS_PER_PAGE,
      };
      
      debouncedSearch(params);
    } catch (err: any) {
      console.error('Search error:', err);
      toast.error('Search failed');
      setIsSearching(false);
    }
  };

  const handlePageChange = (newPage: number) => {
    if (newPage < 0 || newPage >= totalPages) return;
    setSearchParams((prev) => ({ ...prev, page: newPage, size: ITEMS_PER_PAGE }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const isFilterEmpty = useMemo(
    () =>
      !watchedValues.query?.trim() &&
      !watchedValues.location?.trim() &&
      !watchedValues.jobType &&
      !watchedValues.minSalary &&
      !watchedValues.maxSalary &&
      !watchedValues.skills,
    [watchedValues]
  );

  const clearFilters = () => {
    ['query', 'location', 'jobType', 'minSalary', 'maxSalary', 'skills'].forEach((f) => 
      setValue(f as any, '')
    );
    setSearchParams({ page: 0, size: ITEMS_PER_PAGE });
    setHasSearched(false);
    localStorage.removeItem("lastQuery");
    localStorage.removeItem("lastLocation");
    toast.success('Filters cleared');
  };

  const getPageNumbers = () => {
    const pages: number[] = [];
    const maxVisible = 5;
    
    if (totalPages <= maxVisible) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else if (currentPage <= 2) {
      for (let i = 0; i < maxVisible; i++) pages.push(i);
    } else if (currentPage >= totalPages - 3) {
      for (let i = totalPages - maxVisible; i < totalPages; i++) pages.push(i);
    } else {
      for (let i = currentPage - 2; i <= currentPage + 2; i++) pages.push(i);
    }
    
    return pages;
  };

  const isJobSaved = (id: string) => savedJobIds.has(id);
  const isJobApplied = (id: string) => appliedJobs.has(id);

  if (!isAuthenticated) return <div>Loading...</div>;

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Job Search</h1>
              <p className="mt-1 text-gray-600">Find your next opportunity</p>
            </div>
            <Link
              href="/jobs/saved"
              className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            >
              <BookmarkIcon className="-ml-1 mr-2 h-5 w-5" />
              Saved Jobs ({savedJobs.length})
            </Link>
          </div>
        </div>

        {/* Search Filters */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="p-6">
            {/* Main Search Bar */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3 mb-4">
              <div className="md:col-span-1">
                <label htmlFor="query" className="block text-sm font-medium text-gray-900 mb-1">
                  <MagnifyingGlassIcon className="inline-flex h-4 w-4 mr-1 text-gray-700" />
                  Job Title / Keywords
                </label>
                <input
                  {...register('query')}
                  type="text"
                  placeholder="e.g. Software Engineer"
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
              </div>

              <div>
                <label htmlFor="location" className="block text-sm font-medium text-gray-900 mb-1">
                  <MapPinIcon className="inline-flex h-4 w-4 mr-1 text-gray-700" />
                  Location
                </label>
                <input
                  {...register('location')}
                  type="text"
                  placeholder="e.g. Toronto, ON"
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
              </div>

              <div>
                <label htmlFor="jobType" className="block text-sm font-medium text-gray-900 mb-1">
                  Job Type
                </label>
                <select
                  {...register('jobType')}
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                >
                  {JOB_TYPES.map(type => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>


            {/* Advanced Filters Toggle */}
            {/*}
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-500 mb-4"
            >
              <AdjustmentsHorizontalIcon className="h-5 w-5 mr-1" />
              {showFilters ? 'Hide' : 'Show'} Advanced Filters
            </button>
            */}

            {/* Advanced Filters */}
            {/*
            {showFilters && (
              <div className="border-t border-gray-200 pt-4">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                  <div>
                    <label htmlFor="minSalary" className="block text-sm font-medium text-gray-900 mb-1">
                      <CurrencyDollarIcon className="inline-flex h-4 w-4 mr-1 text-gray-700" />
                      Min Salary ($)
                    </label>
                    <input
                      {...register('minSalary')}
                      type="number"
                      placeholder="50000"
                      min="0"
                      step="1000"
                      className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  </div>

                  <div>
                    <label htmlFor="maxSalary" className="block text-sm font-medium text-gray-900 mb-1">
                      <CurrencyDollarIcon className="inline-flex h-4 w-4 mr-1 text-gray-700" />
                      Max Salary ($)
                    </label>
                    <input
                      {...register('maxSalary')}
                      type="number"
                      placeholder="150000"
                      min="0"
                      step="1000"
                      className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  </div>

                  <div>
                    <label htmlFor="skills" className="block text-sm font-medium text-gray-900 mb-1">
                      Skills (comma separated)
                    </label>
                    <input
                      {...register('skills')}
                      type="text"
                      placeholder="React, Node.js, Python"
                      className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  </div>
                </div>
              </div>
            )}
              */}


            {/* Search Buttons */}
            <div className="mt-6 flex flex-col sm:flex-row gap-3">
              <button
                onClick={handleSearch}
                disabled={isSearching || isFilterEmpty}
                className="flex-1 sm:flex-initial inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-semibold rounded-lg text-white bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                {isSearching ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Searching...
                  </>
                ) : (
                  <>
                    <MagnifyingGlassIcon className="w-5 h-5 mr-2" />
                    Search Jobs
                  </>
                )}
              </button>

              <button
                onClick={clearFilters}
                disabled={isSearching}
                className="flex-1 sm:flex-initial inline-flex items-center justify-center px-6 py-3 border-2 border-gray-300 text-base font-semibold rounded-lg text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 shadow-sm hover:shadow-md transform hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                <XMarkIcon className="w-5 h-5 mr-2" />
                Clear All Filters
              </button>

              {(watchedValues.query || watchedValues.location || watchedValues.jobType ||
                watchedValues.minSalary || watchedValues.maxSalary || watchedValues.skills) && (
                <div className="flex items-center text-sm text-gray-500">
                  <FunnelIcon className="w-4 h-4 mr-1" />
                  <span className="font-medium">Active filters</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Results Section */}
        {hasSearched && (
          <>
            {!isLoading && !error && (
              <div className="flex items-center justify-between mb-4">
                <div className="text-sm text-gray-700">
                  {totalElements > 0 ? (
                    <>
                      Showing <span className="font-medium">{currentPage * ITEMS_PER_PAGE + 1}</span> to{' '}
                      <span className="font-medium">
                        {Math.min((currentPage + 1) * ITEMS_PER_PAGE, totalElements)}
                      </span>{' '}
                      of <span className="font-medium">{totalElements}</span> results
                    </>
                  ) : (
                    'No results found'
                  )}
                </div>
                {isFetching && (
                  <div className="flex items-center text-sm text-gray-500">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-indigo-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Updating...
                  </div>
                )}
              </div>
            )}

            {/* Job Results */}
            <div className="bg-white shadow rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                {isLoading && (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {[...Array(6)].map((_, i) => (
                      <div key={i} className="animate-pulse">
                        <div className="border border-gray-200 rounded-lg p-6">
                          <div className="flex items-start justify-between mb-4">
                            <div className="flex-1">
                              <div className="h-5 bg-gray-200 rounded w-3/4 mb-2"></div>
                              <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                            </div>
                            <div className="h-8 w-8 bg-gray-200 rounded"></div>
                          </div>
                          <div className="space-y-2">
                            <div className="h-3 bg-gray-200 rounded"></div>
                            <div className="h-3 bg-gray-200 rounded w-5/6"></div>
                          </div>
                          <div className="mt-4 flex items-center justify-between">
                            <div className="h-6 bg-gray-200 rounded w-20"></div>
                            <div className="h-8 bg-gray-200 rounded w-24"></div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {error && (
                  <div className="text-center py-12">
                    <div className="text-red-500 mb-2">
                      <MagnifyingGlassIcon className="mx-auto h-12 w-12" />
                    </div>
                    <h3 className="text-lg font-medium text-gray-900 mb-2">Search Error</h3>
                    <p className="text-gray-500">
                      There was an error searching for jobs. Please try again.
                    </p>
                  </div>
                )}

                {!isLoading && !error && jobs.length === 0 && (
                  <div className="text-center py-12">
                    <MagnifyingGlassIcon className="mx-auto h-12 w-12 text-gray-400" />
                    <h3 className="mt-2 text-lg font-medium text-gray-900">No jobs found</h3>
                    <p className="mt-1 text-gray-500">
                      Try adjusting your search criteria to find more opportunities.
                    </p>
                    <button
                      onClick={clearFilters}
                      className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                    >
                      Clear Filters
                    </button>
                  </div>
                )}

                {!isLoading && !error && jobs.length > 0 && (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {jobs.map((job) => (
                      <div
                        key={job.id}
                        className="border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow duration-200 relative"
                      >
                        <button
                          onClick={() => handleSaveJob(job.id)}
                          disabled={saveJobMutation.isPending || unsaveJobMutation.isPending}
                          className="absolute top-4 right-4 p-2 text-gray-400 hover:text-indigo-600 transition-colors disabled:opacity-50"
                          title={isJobSaved(job.id) ? 'Remove from saved' : 'Save job'}
                        >
                          {isJobSaved(job.id) ? (
                            <BookmarkIconSolid className="h-6 w-6 text-indigo-600" />
                          ) : (
                            <BookmarkIcon className="h-6 w-6" />
                          )}
                        </button>

                        <div className="pr-8">
                          <Link href={`/jobs/${job.id}`}>
                            <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2 hover:text-indigo-600 cursor-pointer transition-colors">
                              {job.title}
                            </h3>
                          </Link>
                          
                          <div className="flex items-center text-sm text-gray-600 mb-2">
                            <BriefcaseIcon className="h-4 w-4 mr-1 flex-shrink-0" />
                            <span className="truncate">{job.company}</span>
                          </div>
                          
                          {job.location && (
                            <div className="flex items-center text-sm text-gray-500 mb-3">
                              <MapPinIcon className="h-4 w-4 mr-1 flex-shrink-0" />
                              <span className="truncate">{job.location}</span>
                            </div>
                          )}

                          <p className="text-sm text-gray-700 mb-4 line-clamp-3">
                            {job.description}
                          </p>

                          <div className="space-y-2 mb-4">
                            {job.jobType && (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                {job.jobType}
                              </span>
                            )}
                            {job.experienceLevel && (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800 ml-2">
                                {job.experienceLevel}
                              </span>
                            )}
                          </div>

                          {(job.salary || job.salaryRange) && (
                            <div className="flex items-center text-sm text-green-600 font-medium mb-3">
                              <CurrencyDollarIcon className="h-4 w-4 mr-1" />
                              {job.salary && !isNaN(Number(job.salary)) 
                                ? formatSalary(Number(job.salary))
                                : job.salary || job.salaryRange}
                            </div>
                          )}

                          {job.skills && job.skills.length > 0 && (
                            <div className="mb-4">
                              <div className="flex flex-wrap gap-1">
                                {job.skills.slice(0, 3).map((skill, index) => (
                                  <span
                                    key={index}
                                    className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800"
                                  >
                                    {skill}
                                  </span>
                                ))}
                                {job.skills.length > 3 && (
                                  <span className="text-xs text-gray-500 self-center">
                                    +{job.skills.length - 3} more
                                  </span>
                                )}
                              </div>
                            </div>
                          )}

                          {job.postedDate && (
                            <div className="flex items-center text-xs text-gray-400 mb-4">
                              <CalendarIcon className="h-3 w-3 mr-1" />
                              Posted {timeAgo(job.postedDate)}
                            </div>
                          )}

                          <div className="mt-4 flex flex-col gap-2">
                            {job.applyUrl ? (
                              
                                <a href={job.applyUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex-1 sm:flex-none inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm"
                              >
                                <ArrowTopRightOnSquareIcon className="-ml-1 mr-2 h-5 w-5" />
                                Apply on Company Site
                              </a>
                            ) : (
                              <>
                                <button
                                  className="btn-disabled cursor-not-allowed text-gray-600"
                                  disabled
                                  title="No direct application link available"
                                >
                                  ❌ No Apply Link Available
                                </button>
                                
                                <a href={`https://www.google.com/search?q=${encodeURIComponent(job.company || '')}`}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="flex-1 sm:flex-none inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm"
                                  title="Search for this job on Google"
                                >
                                  🔍 Search Company
                                </a>
                              </>
                            )}

                            <button
                              onClick={() => handleMarkAsApplied(job)}
                              disabled={markAsAppliedMutation.isPending || isJobApplied(job.id)}
                              className={`inline-flex items-center justify-center px-4 py-2 text-sm font-medium rounded-md shadow-sm transition-all duration-200 ${
                                isJobApplied(job.id)
                                  ? 'text-green-700 bg-green-100 cursor-not-allowed border border-green-300'
                                  : 'text-white bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 border border-transparent'
                              } disabled:opacity-50 disabled:cursor-not-allowed`}
                            >
                              {markAsAppliedMutation.isPending ? (
                                <>
                                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                  </svg>
                                  Marking...
                                </>
                              ) : isJobApplied(job.id) ? (
                                <>
                                  <CheckCircleIcon className="w-4 h-4 mr-2" />
                                  Applied ✓
                                </>
                              ) : (
                                <>
                                  <CheckCircleIcon className="w-4 h-4 mr-2" />
                                  Mark as Applied
                                </>
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Pagination */}
            {!isLoading && !error && jobs.length > 0 && totalPages > 1 && (
              <div className="bg-white shadow rounded-lg mt-6 px-4 py-3 flex items-center justify-between sm:px-6">
                <div className="flex-1 flex justify-between sm:hidden">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0 || isFetching}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1 || isFetching}
                    className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
                <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm text-gray-700">
                      Page <span className="font-medium">{currentPage + 1}</span> of{' '}
                      <span className="font-medium">{totalPages}</span>
                    </p>
                  </div>
                  <div>
                    <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                      <button
                        onClick={() => handlePageChange(currentPage - 1)}
                        disabled={currentPage === 0 || isFetching}
                        className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <span className="sr-only">Previous</span>
                        <ChevronLeftIcon className="h-5 w-5" />
                      </button>
                      
                      {getPageNumbers().map((pageNum) => (
                        <button
                          key={pageNum}
                          onClick={() => handlePageChange(pageNum)}
                          disabled={isFetching}
                          className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium cursor-pointer transition-colors ${
                            pageNum === currentPage
                              ? 'z-10 bg-indigo-50 border-indigo-500 text-indigo-600'
                              : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                          } disabled:opacity-50 disabled:cursor-not-allowed`}
                        >
                          {pageNum + 1}
                        </button>
                      ))}

                      <button
                        onClick={() => handlePageChange(currentPage + 1)}
                        disabled={currentPage >= totalPages - 1 || isFetching}
                        className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <span className="sr-only">Next</span>
                        <ChevronRightIcon className="h-5 w-5" />
                      </button>
                    </nav>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}