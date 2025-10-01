/* eslint-disable @typescript-eslint/no-explicit-any */
// src/app/applications/new/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Application, ApplicationStatus } from '@/lib/types';
import { useForm } from 'react-hook-form';
import { useRouter, useSearchParams } from 'next/navigation';
import toast from 'react-hot-toast';
import {
  BuildingOfficeIcon,
  BriefcaseIcon,
  MapPinIcon,
  CurrencyDollarIcon,
  DocumentTextIcon,
  LinkIcon,
  UserIcon,
  CalendarIcon,
  PaperClipIcon,
} from '@heroicons/react/24/outline';

interface ApplicationForm {
  companyName: string;
  jobTitle: string;
  jobLocation?: string;
  jobDescription?: string;
  jobLink?: string;
  recruiterContact?: string;
  status: ApplicationStatus;
  salary?: string;
  notes?: string;
  appliedDate?: string;
  referral?: string;
  interviewDate?: string;
  assessmentDate?: string;
}

export default function NewApplicationPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const jobId = searchParams.get('jobId');

  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm<ApplicationForm>({
    defaultValues: {
      status: ApplicationStatus.APPLIED,
      appliedDate: new Date().toISOString().split('T')[0],
    },
  });

  const watchedStatus = watch('status');

  // Fetch job details if jobId is provided
  const { data: jobDetails } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => apiClient.getJob(jobId!),
    enabled: !!jobId,
  });

  // Pre-fill form with job details
  useEffect(() => {
    if (jobDetails) {
      setValue('companyName', jobDetails.company);
      setValue('jobTitle', jobDetails.title);
      setValue('jobLocation', jobDetails.location);
      setValue('jobDescription', jobDetails.description);
      setValue('jobLink', jobDetails.applyUrl || '');
      setValue('salary', jobDetails.salary || jobDetails.salaryRange || '');
    }
  }, [jobDetails, setValue]);

  const createApplicationMutation = useMutation({
    mutationFn: (data: ApplicationForm) => {
      const applicationData: Omit<Application, 'id' | 'userId'> = {
        companyName: data.companyName,
        jobTitle: data.jobTitle,
        jobLocation: data.jobLocation,
        jobDescription: data.jobDescription,
        jobLink: data.jobLink,
        recruiterContact: data.recruiterContact,
        status: data.status,
        salary: data.salary,
        notes: data.notes,
        appliedDate: data.appliedDate,
        referral: data.referral,
        interviewDate: data.interviewDate,
        assessmentDate: data.assessmentDate,
      };
      return apiClient.createApplication(applicationData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      toast.success('Application created successfully!');
      router.push('/applications');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create application');
    },
  });

  const onSubmit = (data: ApplicationForm) => {
    createApplicationMutation.mutate(data);
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-3xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <h1 className="text-2xl font-bold text-gray-900">Add New Application</h1>
          <p className="mt-1 text-gray-600">
            Track your job application and stay organized
          </p>
        </div>

        {/* Form */}
        <div className="bg-white shadow rounded-lg">
          <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-6">
            {/* Basic Information */}
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <BuildingOfficeIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Company Name *
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('companyName', { required: 'Company name is required' })}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="e.g. Google, Microsoft"
                  />
                </div>
                {errors.companyName && (
                  <p className="mt-1 text-sm text-red-600">{errors.companyName.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <BriefcaseIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Job Title *
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('jobTitle', { required: 'Job title is required' })}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="e.g. Senior Software Engineer"
                  />
                </div>
                {errors.jobTitle && (
                  <p className="mt-1 text-sm text-red-600">{errors.jobTitle.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <MapPinIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Location
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('jobLocation')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="e.g. San Francisco, CA"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <CurrencyDollarIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Salary
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('salary')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="e.g. $120,000 or 100k-150k"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  Application Status *
                </label>
                <select
                  {...register('status', { required: 'Status is required' })}
                  className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                >
                  <option value={ApplicationStatus.APPLIED}>Applied</option>
                  <option value={ApplicationStatus.INTERVIEW}>Interview</option>
                  <option value={ApplicationStatus.ASSESSMENT}>Assessment</option>
                  <option value={ApplicationStatus.OFFER}>Offer</option>
                  <option value={ApplicationStatus.REJECTED}>Rejected</option>
                  <option value={ApplicationStatus.WITHDRAWN}>Withdrawn</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Applied Date
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('appliedDate')}
                    type="date"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Conditional Fields */}
            {watchedStatus === ApplicationStatus.INTERVIEW && (
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Interview Date
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('interviewDate')}
                    type="datetime-local"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            )}

            {watchedStatus === ApplicationStatus.ASSESSMENT && (
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Assessment Date
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('assessmentDate')}
                    type="datetime-local"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <LinkIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Job Link
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('jobLink')}
                    type="url"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="https://..."
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <PaperClipIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                  Recruiter Contact
                </label>
                <div className="mt-1 flex">
                  <input
                    {...register('recruiterContact')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="recruiter@company.com"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900">
                <UserIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                Referral
              </label>
              <div className="mt-1 flex">
                <input
                  {...register('referral')}
                  type="text"
                  className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  placeholder="Who referred you?"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900">
                <DocumentTextIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                Job Description
              </label>
              <div className="mt-1">
                <textarea
                  {...register('jobDescription')}
                  rows={4}
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  placeholder="Paste the job description here..."
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900">
                <DocumentTextIcon className="flex left-3 top-3 h-5 w-5 text-gray-700" />
                Notes
              </label>
              <div className="mt-1">
                <textarea
                  {...register('notes')}
                  rows={3}
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  placeholder="Any additional notes or thoughts..."
                />
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center justify-end space-x-3 pt-6 border-t border-gray-200">
              <button
                type="button"
                onClick={() => router.back()}
                className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-900 bg-white hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={createApplicationMutation.isPending}
                className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50"
              >
                {createApplicationMutation.isPending ? 'Creating...' : 'Create Application'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}