/* eslint-disable @typescript-eslint/no-explicit-any */
// src/app/applications/[id]/edit/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Application, ApplicationStatus } from '@/lib/types';
import { useForm } from 'react-hook-form';
import { useRouter, useParams } from 'next/navigation';
import toast from 'react-hot-toast';
import {
  BuildingOfficeIcon,
  BriefcaseIcon,
  MapPinIcon,
  CurrencyDollarIcon,
  LinkIcon,
  UserIcon,
  CalendarIcon,
  PencilSquareIcon,
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
  assessmentDeadline?: string;
}

export default function EditApplicationPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const params = useParams();
  const queryClient = useQueryClient();
  const applicationId = params.id as string;

  const { register, handleSubmit, formState: { errors }, setValue, watch, reset } = useForm<ApplicationForm>();
  const watchedStatus = watch('status');

  // Fetch application details
  const { data: application, isLoading } = useQuery({
    queryKey: ['application', applicationId],
    queryFn: () => apiClient.getApplication(applicationId),
    enabled: isAuthenticated && !!applicationId,
  });

  // Populate form when application data is loaded
  useEffect(() => {
    if (application) {
      reset({
        companyName: application.companyName,
        jobTitle: application.jobTitle,
        jobLocation: application.jobLocation || '',
        jobDescription: application.jobDescription || '',
        jobLink: application.jobLink || '',
        recruiterContact: application.recruiterContact || '',
        status: application.status,
        salary: application.salary ? String(application.salary) : '',
        notes: application.notes || '',
        appliedDate: application.appliedDate?.split('T')[0] || '',
        referral: application.referral || '',
        interviewDate: application.interviewDate?.slice(0, 16) || '',
        assessmentDeadline: application.assessmentDeadline?.slice(0, 16) || '',
      });
    }
  }, [application, reset]);

  const updateApplicationMutation = useMutation({
    mutationFn: (data: ApplicationForm) => {
      const updateData: Partial<Application> = {
        userId: application?.userId,
        companyName: data.companyName,
        jobTitle: data.jobTitle,
        jobLocation: data.jobLocation,
        jobDescription: data.jobDescription,
        jobLink: data.jobLink,
        recruiterContact: data.recruiterContact,
        status: data.status,
        salary: data.salary ? Number(data.salary) : undefined,
        notes: data.notes,
        appliedDate: data.appliedDate,
        referral: data.referral,
        interviewDate: data.interviewDate,
        assessmentDeadline: data.assessmentDeadline,
      };
      return apiClient.updateApplication(applicationId, updateData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      queryClient.invalidateQueries({ queryKey: ['application', applicationId] });
      toast.success('Application updated successfully!');
      router.push(`/applications/${applicationId}`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update application');
    },
  });

  const onSubmit = (data: ApplicationForm) => {
    updateApplicationMutation.mutate(data);
  };

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-3xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/3 mb-4"></div>
            <div className="bg-white shadow rounded-lg p-6">
              <div className="grid grid-cols-2 gap-4">
                {[...Array(8)].map((_, i) => (
                  <div key={i}>
                    <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                    <div className="h-10 bg-gray-200 rounded"></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-3xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <h1 className="text-2xl font-bold text-gray-900">Edit Application</h1>
          <p className="mt-1 text-gray-600">
            Update your application details
          </p>
        </div>

        {/* Form */}
        <div className="bg-white shadow rounded-lg">
          <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-6">
            {/* Basic Information */}
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <BuildingOfficeIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Company Name*
                  </label>
                  <input
                    {...register('companyName', { required: 'Company name is required' })}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
                {errors.companyName && (
                  <p className="mt-1 text-sm text-red-600">{errors.companyName.message}</p>
                )}
              </div>

              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <BriefcaseIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Job Title *
                  </label>
                  <input
                    {...register('jobTitle', { required: 'Job title is required' })}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
                {errors.jobTitle && (
                  <p className="mt-1 text-sm text-red-600">{errors.jobTitle.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <MapPinIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Location
                  </label>
                  <input
                    {...register('jobLocation')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <CurrencyDollarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Salary
                  </label>
                  <input
                    {...register('salary')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-900">
                  <PencilSquareIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
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
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Applied Date
                  </label>
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
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Interview Date
                  </label>
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
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <CalendarIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Assessment Date
                  </label>
                  <input
                    {...register('assessmentDeadline')}
                    type="datetime-local"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <LinkIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Job Link
                  </label>
                  <input
                    {...register('jobLink')}
                    type="url"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <div className="mt-1 relative">
                  <label className="block text-sm font-medium text-gray-900">
                  <UserIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                    Recruiter Contact
                  </label>
                  <input
                    {...register('recruiterContact')}
                    type="text"
                    className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
              </div>
            </div>

            <div>
              <div className="mt-1 relative">
                <label className="block text-sm font-medium text-gray-900">
                  <UserIcon className="inline-flex left-3 top-3 h-4 w-4 mr-1 text-gray-700" />
                  Referral
                </label>
                <input
                  {...register('referral')}
                  type="text"
                  className="text-gray-900 pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900">
                Job Description
              </label>
              <div className="mt-1">
                <textarea
                  {...register('jobDescription')}
                  rows={4}
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-900">
                Notes
              </label>
              <div className="mt-1">
                <textarea
                  {...register('notes')}
                  rows={3}
                  className="text-gray-900 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
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
                disabled={updateApplicationMutation.isPending}
                className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50"
              >
                {updateApplicationMutation.isPending ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
