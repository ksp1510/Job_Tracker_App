/* eslint-disable react-hooks/rules-of-hooks */
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
import Cookies from 'js-cookie';
import toast from 'react-hot-toast';
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
  const [hasCachedResults, setHasCachedResults] = useState(false);
  const [usingCacheData, setUsingCacheData] = useState(false);
  const [searchParams, setSearchParams] = useState<JobSearchParams>({
    page: 0,
    size: ITEMS_PER_PAGE,
  });
  
  useEffect(() => {
    initializeComponent();
  }, []);

  const initializeComponent = async () => {
    const hasCached = await apiClient.checkCacheStatus();
    setHasCachedResults(hasCached);
    if (hasCached) {
      await loadCachedResults();
    }
  };

  /**
   * Load cached results (on app return)
   */
  const loadCachedResults = async () => {
    try {
      const result = await apiClient.getCachedResults(
        searchParams.page,
        searchParams.size
      );
      if (!result) {
        setHasCachedResults(false);
        return;
      }
      // Seed react-query cache with cached results so the UI renders from cache
      queryClient.setQueryData(['jobs', searchParams], result);
      setUsingCacheData(true);
      console.log('Using cached results');
    } catch (error) {
      console.error('Failed to load cached results:', error);
      setHasCachedResults(false);
    }
  };

  /**
   * Clear cached results
   */
  const handleClearCache = async () => {
    await apiClient.clearCache();
    setHasCachedResults(false);
    setUsingCacheData(false);
    queryClient.invalidateQueries({ queryKey: ['jobs', searchParams] });
    console.log('Cache cleared successfully');
  };

  /**
   * Handle input changes
   */
  const handleInputChange = (e: { target: { name: any; value: any; }; }) => {
    const { name, value } = e.target;
    setSearchParams(prev => ({
      ...prev,
      [name]: value,
    }));
  };


  const { register, watch, setValue } = useForm<{
    query: string;
    location: string;
    jobType: string;
    minSalary: string;
    maxSalary: string;
    skills: string;
  }>({
    defaultValues: {
      query: '',
      location: '',
      jobType: '',
      minSalary: '',
      maxSalary: '',
      skills: '',
    }
  });

  // Watch form values for real-time search
  const watchedValues = watch();

  // Debounced search effect
  useEffect(() => {
    const debouncedSearch = debounce(() => {
      const skillsArray = watchedValues.skills 
        ? watchedValues.skills.split(',').map(s => s.trim()).filter(s => s)
        : undefined;

      const newParams: JobSearchParams = {
        query: watchedValues.query?.trim() || undefined,
        location: watchedValues.location?.trim() || undefined,
        jobType: watchedValues.jobType || undefined,
        minSalary: watchedValues.minSalary ? parseFloat(watchedValues.minSalary) : undefined,
        maxSalary: watchedValues.maxSalary ? parseFloat(watchedValues.maxSalary) : undefined,
        skills: skillsArray,
        page: 0, // Reset to first page on search
        size: ITEMS_PER_PAGE,
      };
      setSearchParams(newParams);
    }, 500);

    debouncedSearch();
  }, [watchedValues]);

  // Fetch jobs
  const { data: jobsResponse, isLoading, error, isFetching } = useQuery({
    queryKey: ['jobs', searchParams],
    queryFn: () => apiClient.searchJobs(searchParams),
    enabled: isAuthenticated,
    placeholderData: (prev) => prev,
  });

  // Fetch saved jobs to mark as saved
  const { data: savedJobs = [] as SavedJob[] } = useQuery<SavedJob[]>({
    queryKey: ['saved-jobs'],
    queryFn: () => apiClient.getSavedJobs(),
    enabled: isAuthenticated,
  });

  //External search mutation
  const externalSearchMutation = useMutation({
    mutationFn: async (params: JobSearchParams) => {
      // Call your backend API that uses external job search
      const response = await apiClient.searchJobs(params);
      return response;
    },
    onSuccess: (data) => {
      toast.success(`Found ${data.totalElements} jobs`);
      setIsSearching(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Search failed');
      setIsSearching(false);
    },
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

  //Mark as applied mutation
  const markAsAppliedMutation = useMutation({
    mutationFn: async (job: JobListing) => {
      const uId = await apiClient.getCurrentUser();
      console.log(uId.userId);
      //Create a new application directly with job details
      return apiClient.createApplication({
        userId: uId.userId,
        companyName: job.company,
        jobTitle: job.title,
        jobLocation: job.location,
        jobDescription: job.description,
        jobLink: job.applyUrl || undefined,
        status: 'APPLIED' as any,
        salary: job.salary || job.salaryRange || undefined,
        appliedDate: new Date().toISOString(),
        notes: `Applied via JobTracker on ${new Date().toLocaleDateString()}`,
        id: ''
      });
    },
    onSuccess: (data, job) => {
      queryClient.invalidateQueries({queryKey: ['applications'] });
      setAppliedJobs(prev => new Set([...prev, job.id]));
      toast.success('Job marked as applied');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to mark job as applied');
    }
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

  //Check if job is already applied
  const isJobApplied = (jobId: string) => {
    return appliedJobs.has(jobId);
  };

  // Handle mark as applied
  const handleMarkAsApplied = (job: JobListing) => {
    if (window.confirm(`Mark "${job.title}" at ${job.company} as applied?`)) {
      markAsAppliedMutation.mutate(job);
    }
  };

  // Handle search button click
  const handleSearch = () => {
    setIsSearching(true);
    
    const skillsArray = watchedValues.skills
      ? watchedValues.skills.split(',').map(s => s.trim()).filter(s => s)
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
    externalSearchMutation.mutate(params);

  }

  const handlePageChange = (newPage: number) => {
    setSearchParams(prev => ({ ...prev, page: newPage }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const clearFilters = () => {
    setValue('query', '');
    setValue('location', '');
    setValue('jobType', '');
    setValue('minSalary', '');
    setValue('maxSalary', '');
    setValue('skills', '');
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  const jobs = jobsResponse?.content || [];
  const totalPages = jobsResponse?.totalPages || 0;
  const currentPage = jobsResponse?.page || 0;
  const totalElements = jobsResponse?.totalElements || 0;

  // Calculate visible page numbers
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    
    if (totalPages <= maxVisible) {
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (currentPage <= 2) {
        for (let i = 0; i < maxVisible; i++) {
          pages.push(i);
        }
      } else if (currentPage >= totalPages - 3) {
        for (let i = totalPages - maxVisible; i < totalPages; i++) {
          pages.push(i);
        }
      } else {
        for (let i = currentPage - 2; i <= currentPage + 2; i++) {
          pages.push(i);
        }
      }
    }
    
    return pages;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Job Search</h1>
              <p className="mt-1 text-gray-600">
                Find your next opportunity
              </p>
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

        {/* Search and Filter Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="p-6">
            {/* Main Search Bar */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3 mb-4">
              <div className="md:col-span-1">
                <label htmlFor="query" className="block text-sm font-medium text-gray-900 mb-1">
                  <MagnifyingGlassIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                  Job Title / Keywords
                </label>
                <div className="relative">
                  <input
                    {...register('query')}
                    type="text"
                    placeholder="e.g. Software Engineer"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="location" className="block text-sm font-medium text-gray-900 mb-1">
                  <MapPinIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                  Location
                </label>
                <div className="relative">
                  <input
                    {...register('location')}
                    type="text"
                    placeholder="e.g. San Francisco, CA"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
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
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-500 mb-4"
            >
              <AdjustmentsHorizontalIcon className="h-5 w-5 mr-1" />
              {showFilters ? 'Hide' : 'Show'} Advanced Filters
            </button>

            {/* Advanced Filters */}
            {showFilters && (
              <div className="border-t border-gray-200 pt-4">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                  <div>
                    <label htmlFor="minSalary" className="block text-sm font-medium text-gray-900 mb-1">
                    <CurrencyDollarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                      Min Salary ($)
                    </label>
                    <div className="relative">
                      
                      <input
                        {...register('minSalary')}
                        type="number"
                        placeholder="50000"
                        min="0"
                        step="1000"
                        className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    </div>
                  </div>

                  <div>
                    <label htmlFor="maxSalary" className="block text-sm font-medium text-gray-900 mb-1">
                    <CurrencyDollarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                      Max Salary ($)
                    </label>
                    <div className="relative">
                      <input
                        {...register('maxSalary')}
                        type="number"
                        placeholder="150000"
                        min="0"
                        step="1000"
                        className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    </div>
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
              
                <div className="mt-6 flex flex-col sm:flex-row gap-3">
                  {/* Search Button - Primary Action */}
                  <button
                    onClick={handleSearch}
                    disabled={isSearching || externalSearchMutation.isPending}
                    className="flex-1 sm:flex-initial inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-semibold rounded-lg text-gray-900 bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                  >
                    {isSearching || externalSearchMutation.isPending ? (
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

                  {/* Clear Filters Button - Secondary Action */}
                  <button
                    onClick={clearFilters}
                    disabled={isSearching || externalSearchMutation.isPending}
                    className="flex-1 sm:flex-initial inline-flex items-center justify-center px-6 py-3 border-2 border-gray-300 text-base font-semibold rounded-lg text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 shadow-sm hover:shadow-md transform hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                    >
                    <XMarkIcon className="w-5 h-5 mr-2" />
                    Clear All Filters
                  </button>

                  {/* Optional: Show active filters count */}
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

        {/* Results Header */}
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
            {/* Loading State */}
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
                <MagnifyingGlassIcon className="mx-auto h-12 w-12 text-gray-700" />
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

            {/* Job Grid */}
            {!isLoading && !error && jobs.length > 0 && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {jobs.map((job: JobListing) => (
                  <div
                    key={job.id}
                    className="border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow duration-200 relative"
                  >
                    {/* Save Button */}
                    <button
                      onClick={() => handleSaveJob(job)}
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

                    {/* Job Content */}
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

                      {/* Job Details */}
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

                      {/* Salary */}
                      {(job.salary || job.salaryRange) && (
                        <div className="flex items-center text-sm text-green-600 font-medium mb-3">
                          <CurrencyDollarIcon className="h-4 w-4 mr-1" />
                          {job.salary && !isNaN(Number(job.salary)) 
                            ? formatSalary(Number(job.salary))
                            : job.salary || job.salaryRange}
                        </div>
                      )}

                      {/* Skills */}
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

                      {/* Posted Date */}
                      {job.postedDate && (
                        <div className="flex items-center text-xs text-gray-400 mb-4">
                          <CalendarIcon className="h-3 w-3 mr-1" />
                          Posted {timeAgo(job.postedDate)}
                        </div>
                      )}

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

                        {/* Mark as Applied */}
                        <button
                          onClick={() => handleMarkAsApplied(job)}
                          disabled={markAsAppliedMutation.isPending || isJobApplied(job.id)}
                          className={`flex-1 inline-flex items-center justify-center px-3 py-2 border border-transparent text-sm font-medium rounded-lg shadow-sm transition-all duration-200 ${
                            isJobApplied(job.id)
                              ? 'text-green-700 bg-green-100 cursor-not-allowed'
                              : 'text-white bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 hover:shadow-md transform hover:-translate-y-0.5'
                          } disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none`}
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
                            <CheckCircleIcon className="w-4 h-4 mr-1.5" />
                            Marked as Applied
                          </>
                        ) : (
                          <>
                            <CheckCircleIcon className="w-4 h-4 mr-1.5" />
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
                disabled={currentPage === 0}
                className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
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
                    disabled={currentPage === 0}
                    className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <span className="sr-only">Previous</span>
                    <ChevronLeftIcon className="h-5 w-5" />
                  </button>
                  
                  {getPageNumbers().map((pageNum) => (
                    <button
                      key={pageNum}
                      onClick={() => handlePageChange(pageNum)}
                      className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                        pageNum === currentPage
                          ? 'z-10 bg-indigo-50 border-indigo-500 text-indigo-600'
                          : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                      }`}
                    >
                      {pageNum + 1}
                    </button>
                  ))}

                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
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
      </div>
    </div>
  );
}