/* eslint-disable @typescript-eslint/no-explicit-any */
// src/app/notifications/page.tsx
'use client';

import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Notification, Application } from '@/lib/types';
import { formatDateTime, formatDate } from '@/lib/utils';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import {
  BellIcon,
  CalendarIcon,
  ClockIcon,
  XMarkIcon,
  PlusIcon,
  EyeIcon,
  CheckIcon,
} from '@heroicons/react/24/outline';

interface ReminderForm {
  applicationId: string;
  type: 'interview' | 'deadline' | 'custom';
  date: string;
  time: string;
  message: string;
}

export default function NotificationsPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [showReminderForm, setShowReminderForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'unread'>('unread');

  // Fetch notifications
  const { data: allNotifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => apiClient.getNotifications(),
    enabled: isAuthenticated,
  });

  // Fetch unread notifications
  const { data: unreadNotifications = [] } = useQuery({
    queryKey: ['unread-notifications'],
    queryFn: () => apiClient.getUnreadNotifications(),
    enabled: isAuthenticated,
  });

  // Fetch applications for reminder form
  const { data: applications = [] } = useQuery({
    queryKey: ['applications'],
    queryFn: () => apiClient.getApplications(),
    enabled: isAuthenticated && showReminderForm,
  });

  // Form for creating reminders
  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm<ReminderForm>();
  const watchedType = watch('type');

  // Mark as read mutation
  const markAsReadMutation = useMutation({
    mutationFn: (id: string) => apiClient.markNotificationAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: (id: string) => apiClient.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
      toast.success('Notification deleted');
    },
  });

  // Create reminder mutations
  const createInterviewReminderMutation = useMutation({
    mutationFn: (data: { applicationId: string; interviewDate: string; customMessage?: string }) =>
      apiClient.createInterviewReminder(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
      toast.success('Interview reminder created');
      setShowReminderForm(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create reminder');
    },
  });

  const createDeadlineReminderMutation = useMutation({
    mutationFn: (data: { applicationId: string; assessmentDeadline: string; customMessage?: string }) =>
      apiClient.createDeadlineReminder(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
      toast.success('Deadline reminder created');
      setShowReminderForm(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create reminder');
    },
  });

  const createCustomNotificationMutation = useMutation({
    mutationFn: (data: { applicationId?: string; message: string; notifyAt: string }) =>
      apiClient.createCustomNotification(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
      toast.success('Custom reminder created');
      setShowReminderForm(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create reminder');
    },
  });

  const onSubmitReminder = (data: ReminderForm) => {
    const dateTime = `${data.date}T${data.time}:00`;
    
    switch (data.type) {
      case 'interview':
        createInterviewReminderMutation.mutate({
          applicationId: data.applicationId,
          interviewDate: dateTime,
          customMessage: data.message,
        });
        break;
      case 'deadline':
        createDeadlineReminderMutation.mutate({
          applicationId: data.applicationId,
          assessmentDeadline: dateTime,
          customMessage: data.message,
        });
        break;
      case 'custom':
        createCustomNotificationMutation.mutate({
          applicationId: data.applicationId || undefined,
          message: data.message,
          notifyAt: dateTime,
        });
        break;
    }
  };

  const displayedNotifications = activeTab === 'unread' ? unreadNotifications : allNotifications;

  if (!isAuthenticated) {
    return <div>Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-4xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="px-4 py-6 sm:px-0">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Notifications & Reminders</h1>
              <p className="mt-1 text-gray-600">
                Stay on top of your job search with smart reminders
              </p>
            </div>
            <button
              onClick={() => setShowReminderForm(true)}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
            >
              <PlusIcon className="-ml-1 mr-2 h-5 w-5" />
              Create Reminder
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('unread')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'unread'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Unread ({unreadNotifications.length})
            </button>
            <button
              onClick={() => setActiveTab('all')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'all'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              All Notifications ({allNotifications.length})
            </button>
          </nav>
        </div>

        {/* Notifications List */}
        <div className="bg-white shadow rounded-lg">
          {isLoading ? (
            <div className="animate-pulse p-6">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-start space-x-4 py-4 border-b border-gray-200 last:border-b-0">
                  <div className="h-10 w-10 bg-gray-200 rounded-full"></div>
                  <div className="flex-1">
                    <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                    <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                  </div>
                </div>
              ))}
            </div>
          ) : displayedNotifications.length === 0 ? (
            <div className="text-center py-12">
              <BellIcon className="mx-auto h-12 w-12 text-gray-700" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">
                {activeTab === 'unread' ? 'No unread notifications' : 'No notifications yet'}
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                {activeTab === 'unread' 
                  ? "You're all caught up!"
                  : 'Create reminders to stay organized with your job search.'
                }
              </p>
            </div>
          ) : (
            <ul className="divide-y divide-gray-200">
              {displayedNotifications.map((notification: Notification) => (
                <li key={notification.id} className={`p-6 ${!notification.read ? 'bg-blue-50' : ''}`}>
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-3 flex-1">
                      <div className="flex-shrink-0">
                        {notification.type === 'INTERVIEW' ? (
                          <CalendarIcon className="h-6 w-6 text-yellow-500" />
                        ) : notification.type === 'DEADLINE' ? (
                          <ClockIcon className="h-6 w-6 text-red-500" />
                        ) : (
                          <BellIcon className="h-6 w-6 text-blue-500" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-gray-900 mb-1">
                          {notification.message}
                        </p>
                        <p className="text-xs text-gray-500">
                          {formatDateTime(notification.notifyAt)}
                        </p>
                        {notification.type && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800 mt-2">
                            {notification.type.toLowerCase()}
                          </span>
                        )}
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2 ml-4">
                      {!notification.read && (
                        <button
                          onClick={() => markAsReadMutation.mutate(notification.id)}
                          className="p-1 text-gray-900 hover:text-green-600"
                          title="Mark as read"
                        >
                          <CheckIcon className="h-4 w-4" />
                        </button>
                      )}
                      <button
                        onClick={() => deleteNotificationMutation.mutate(notification.id)}
                        className="p-1 text-gray-900 hover:text-red-600"
                        title="Delete notification"
                      >
                        <XMarkIcon className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Create Reminder Modal */}
        {showReminderForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Create Reminder</h3>
                <button
                  onClick={() => setShowReminderForm(false)}
                  className="text-gray-900 hover:text-gray-600"
                >
                  <XMarkIcon className="h-6 w-6" />
                </button>
              </div>

              <form onSubmit={handleSubmit(onSubmitReminder)} className="space-y-4">
                {/* Reminder Type */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    Reminder Type
                  </label>
                  <select
                    {...register('type', { required: 'Please select a reminder type' })}
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  >
                    <option value="">Select type</option>
                    <option value="interview">Interview Reminder</option>
                    <option value="deadline">Assessment Deadline</option>
                    <option value="custom">Custom Reminder</option>
                  </select>
                  {errors.type && (
                    <p className="mt-1 text-sm text-red-600">{errors.type.message}</p>
                  )}
                </div>

                {/* Application Selection */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    Related Application {watchedType === 'custom' ? '(Optional)' : ''}
                  </label>
                  <select
                    {...register('applicationId', { 
                      required: watchedType !== 'custom' ? 'Please select an application' : false 
                    })}
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  >
                    <option value="">Select application</option>
                    {applications.map((app: Application) => (
                      <option key={app.id} value={app.id}>
                        {app.jobTitle} at {app.companyName}
                      </option>
                    ))}
                  </select>
                  {errors.applicationId && (
                    <p className="text-gray-900 mt-1 text-sm text-red-600">{errors.applicationId.message}</p>
                  )}
                </div>

                {/* Date */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    Date
                  </label>
                  <input
                    {...register('date', { required: 'Date is required' })}
                    type="date"
                    min={new Date().toISOString().split('T')[0]}
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                  {errors.date && (
                    <p className="mt-1 text-sm text-red-600">{errors.date.message}</p>
                  )}
                </div>

                {/* Time */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    Time
                  </label>
                  <input
                    {...register('time', { required: 'Time is required' })}
                    type="time"
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                  {errors.time && (
                    <p className="mt-1 text-sm text-red-600">{errors.time.message}</p>
                  )}
                </div>

                {/* Message */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    Custom Message {watchedType !== 'custom' ? '(Optional)' : ''}
                  </label>
                  <textarea
                    {...register('message', { 
                      required: watchedType === 'custom' ? 'Message is required for custom reminders' : false 
                    })}
                    rows={3}
                    placeholder={
                      watchedType === 'interview' 
                        ? 'Don\'t forget your interview tomorrow!'
                        : watchedType === 'deadline'
                        ? 'Complete your assessment soon!'
                        : 'Enter your reminder message...'
                    }
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                  {errors.message && (
                    <p className="mt-1 text-sm text-red-600">{errors.message.message}</p>
                  )}
                </div>

                {/* Action Buttons */}
                <div className="flex items-center justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowReminderForm(false)}
                    className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-900 bg-white hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={
                      createInterviewReminderMutation.isPending ||
                      createDeadlineReminderMutation.isPending ||
                      createCustomNotificationMutation.isPending
                    }
                    className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50"
                  >
                    {(createInterviewReminderMutation.isPending ||
                      createDeadlineReminderMutation.isPending ||
                      createCustomNotificationMutation.isPending) ? (
                      'Creating...'
                    ) : (
                      'Create Reminder'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}