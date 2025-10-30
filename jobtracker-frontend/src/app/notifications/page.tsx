/* eslint-disable @typescript-eslint/no-explicit-any */
// src/app/notifications/page.tsx
'use client';

import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { Notification, Application } from '@/lib/types';
import { formatDateTime } from '@/lib/utils';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import {
  BellIcon,
  CalendarIcon,
  ClockIcon,
  XMarkIcon,
  PlusIcon,
  CheckIcon,
  BuildingOfficeIcon,
  BriefcaseIcon,
  GlobeAltIcon,
} from '@heroicons/react/24/outline';


// Common timezones for dropdown
const TIMEZONES = [
  { value: 'America/New_York', label: 'Eastern Time (ET)' },
  { value: 'America/Chicago', label: 'Central Time (CT)' },
  { value: 'America/Denver', label: 'Mountain Time (MT)' },
  { value: 'America/Los_Angeles', label: 'Pacific Time (PT)' },
  { value: 'America/Anchorage', label: 'Alaska Time (AKT)' },
  { value: 'Pacific/Honolulu', label: 'Hawaii Time (HT)' },
  { value: 'America/Phoenix', label: 'Arizona Time (MST)' },
  { value: 'America/Toronto', label: 'Toronto (ET)' },
  { value: 'America/Vancouver', label: 'Vancouver (PT)' },
  { value: 'America/Halifax', label: 'Halifax (AT)' },
  { value: 'Europe/London', label: 'London (GMT/BST)' },
  { value: 'Europe/Paris', label: 'Paris (CET/CEST)' },
  { value: 'Europe/Berlin', label: 'Berlin (CET/CEST)' },
  { value: 'Asia/Tokyo', label: 'Tokyo (JST)' },
  { value: 'Asia/Shanghai', label: 'Shanghai (CST)' },
  { value: 'Asia/Dubai', label: 'Dubai (GST)' },
  { value: 'Asia/Kolkata', label: 'India (IST)' },
  { value: 'Australia/Sydney', label: 'Sydney (AEDT/AEST)' },
  { value: 'UTC', label: 'UTC (Coordinated Universal Time)' },
];

interface ReminderForm {
  applicationId: string;
  type: 'interview' | 'deadline' | 'custom';
  date: string;
  time: string;
  timezone: string;
  message: string;
}

function formatUtcToLocal(utcString: string) {
  try {
    const date = new Date(utcString + 'Z');
    return date.toLocaleString('en-CA', {
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return utcString;
  }
}

// Helper function to convert date, time, and timezone to ISO 8601 with timezone
function toISO8601WithTimezone(date: string, time: string, timezone: string): string {
  // Combine date and time
  const dateTimeStr = `${date}T${time}`;
  
  // Create a date object in the specified timezone
  // We'll use Intl.DateTimeFormat to help with timezone conversion
  const dateObj = new Date(`${dateTimeStr}:00`);
  
  // Format to ISO 8601 with timezone offset
  // For proper conversion, we need to use a library or calculate offset
  // Here's a simple approach using the timezone
  
  // Get timezone offset in hours
  const getTimezoneOffset = (tz: string, date: Date): string => {
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: tz,
      timeZoneName: 'longOffset'
    });
    
    const parts = formatter.formatToParts(date);
    const offsetPart = parts.find(part => part.type === 'timeZoneName');
    
    if (offsetPart && offsetPart.value.startsWith('GMT')) {
      return offsetPart.value.replace('GMT', '');
    }

    // Fallback: calculate offset manually
    const localDate = new Date(date.toLocaleString('en-US', { timeZone: tz }));
    const utcDate = new Date(date.toLocaleString('en-US', { timeZone: 'UTC' }));
    const diffMs = localDate.getTime() - utcDate.getTime();
    const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.abs(Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60)));
    
    const sign = diffHrs >= 0 ? '+' : '-';
    const hours = String(Math.abs(diffHrs)).padStart(2, '0');
    const minutes = String(diffMins).padStart(2, '0');
    
    return `${sign}${hours}:${minutes}`;
  };
  
  const offset = getTimezoneOffset(timezone, dateObj);
  return `${dateTimeStr}:00${offset}`;
}

// Notification Detail Modal Component
const NotificationDetailModal = ({ 
  notification, 
  application, 
  onClose 
}: { 
  notification: Notification; 
  application?: Application; 
  onClose: () => void;
}) => {
  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center">
      <div className="relative mx-auto p-6 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        {/* Header */}
        <div className="flex items-center justify-between mb-4 pb-4 border-b">
          <h3 className="text-xl font-semibold text-gray-900">
            Notification Details
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        {/* Content */}
        <div className="space-y-4">
          {/* Type Badge */}
          <div className="flex items-center space-x-2">
            {notification.type === 'INTERVIEW' ? (
              <CalendarIcon className="h-6 w-6 text-yellow-500" />
            ) : notification.type === 'DEADLINE' ? (
              <ClockIcon className="h-6 w-6 text-red-500" />
            ) : (
              <BellIcon className="h-6 w-6 text-blue-500" />
            )}
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
              {notification.type}
            </span>
          </div>

          {/* Application Info */}
          {application && (
            <div className="bg-indigo-50 rounded-lg p-4 space-y-2">
              <div className="flex items-center space-x-2">
                <BriefcaseIcon className="h-5 w-5 text-indigo-600" />
                <span className="font-semibold text-gray-900">
                  {application.jobTitle}
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <BuildingOfficeIcon className="h-5 w-5 text-indigo-600" />
                <span className="text-gray-700">
                  {application.companyName}
                </span>
              </div>
            </div>
          )}

          {/* Message */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Message
            </label>
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-gray-900 whitespace-pre-wrap">
                {notification.message}
              </p>
            </div>
          </div>

          {/* Scheduled Time */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Scheduled For
            </label>
            <div className="flex items-center space-x-2 text-gray-900">
              <CalendarIcon className="h-5 w-5 text-gray-500" />
              <span>{formatUtcToLocal(notification.eventDate)}</span>
            </div>
          </div>

          {/* Status */}
          <div className="flex items-center justify-between pt-4 border-t">
            <span className="text-sm text-gray-500">
              Status: {notification.read ? 'Read' : 'Unread'}
            </span>
            <span className="text-sm text-gray-500">
              Sent: {notification.sent ? 'Yes' : 'Pending'}
            </span>
          </div>
        </div>

        {/* Actions */}
        <div className="mt-6 flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default function NotificationsPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [showReminderForm, setShowReminderForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'unread'>('unread');
  const [selectedNotification, setSelectedNotification] = useState<Notification | null>(null);

  const defaultTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  // Form for creating reminders
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<ReminderForm>({
    defaultValues: {
      timezone: defaultTimezone,
    },
  });
  const watchedType = watch('type');

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
    refetchInterval: 30000,
    select: (data) => {
      const now = new Date();
      return data.filter(notification => {
        const notifAt = new Date(notification.notifyAt);
        return notifAt <= now; // Only show notifications that are due
      });
    },
  });

  // Fetch applications for displaying in notifications
  const { data: applications = [] } = useQuery({
    queryKey: ['applications'],
    queryFn: () => apiClient.getApplications(),
    enabled: isAuthenticated,
  });

  

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
      toast.success('Deadline reminder created');
      setShowReminderForm(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create reminder');
    },
  });

  const createCustomNotificationMutation = useMutation({
    mutationFn: (data: { applicationId?: string; message: string; eventDate: string }) =>
      apiClient.createCustomNotification(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Custom reminder created');
      setShowReminderForm(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create reminder');
    },
  });

  const onSubmitReminder = (data: ReminderForm) => {
    const dateTimeWithTz = toISO8601WithTimezone(data.date, data.time, data.timezone);
    
    if (data.type === 'interview') {
      createInterviewReminderMutation.mutate({
        applicationId: data.applicationId,
        interviewDate: dateTimeWithTz,
        customMessage: data.message || undefined,
      });
    } else if (data.type === 'deadline') {
      createDeadlineReminderMutation.mutate({
        applicationId: data.applicationId,
        assessmentDeadline: dateTimeWithTz,
        customMessage: data.message || undefined,
      });
    } else if (data.type === 'custom') {
      createCustomNotificationMutation.mutate({
        applicationId: data.applicationId || undefined,
        eventDate: dateTimeWithTz,
        message: data.message,
      });
    }
  };

  const handleNotificationClick = (notification: Notification) => {
    // Mark as read if unread
    if (!notification.read) {
      markAsReadMutation.mutate(notification.id);
    }
    // Show detail modal
    setSelectedNotification(notification);
  };

  const getApplicationForNotification = (notification: Notification) => {
    return applications.find((app: Application) => app.id === notification.applicationId);
  };

  const displayedNotifications = activeTab === 'unread' 
    ? unreadNotifications.filter(n => new Date(n.notifyAt) <= new Date()) 
    : allNotifications;

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
              {displayedNotifications.map((notification: Notification) => {
                const application = getApplicationForNotification(notification);
                
                return (
                  <li 
                    key={notification.id} 
                    className={`p-6 hover:bg-gray-50 cursor-pointer transition-colors ${
                      !notification.read ? 'bg-blue-50' : ''
                    }`}
                    onClick={() => handleNotificationClick(notification)}
                  >
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
                          {/* Application Info */}
                          {application && (
                            <div className="mb-2">
                              <div className="flex items-center text-sm font-semibold text-gray-900">
                                <BriefcaseIcon className="h-4 w-4 mr-1 text-indigo-600" />
                                {application.jobTitle}
                              </div>
                              <div className="flex items-center text-sm text-gray-600">
                                <BuildingOfficeIcon className="h-4 w-4 mr-1 text-indigo-600" />
                                {application.companyName}
                              </div>
                            </div>
                          )}
                          
                          {/* Message */}
                          <p className="text-sm text-gray-900 mb-1 line-clamp-2">
                            {notification.message}
                          </p>
                          
                          {/* Time and Type */}
                          <div className="flex items-center space-x-3 mt-2">
                            <p className="text-xs text-gray-500">
                              {formatUtcToLocal(notification.eventDate)}
                            </p>
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                              {notification.type.toLowerCase()}
                            </span>
                          </div>
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-2 ml-4">
                        {!notification.read && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              markAsReadMutation.mutate(notification.id);
                            }}
                            className="p-1 text-gray-400 hover:text-green-600"
                            title="Mark as read"
                          >
                            <CheckIcon className="h-4 w-4" />
                          </button>
                        )}
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteNotificationMutation.mutate(notification.id);
                          }}
                          className="p-1 text-gray-400 hover:text-red-600"
                          title="Delete notification"
                        >
                          <XMarkIcon className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Notification Detail Modal */}
        {selectedNotification && (
          <NotificationDetailModal
            notification={selectedNotification}
            application={getApplicationForNotification(selectedNotification)}
            onClose={() => setSelectedNotification(null)}
          />
        )}

        {/* Create Reminder Modal - keeping existing form */}
        {showReminderForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Create Reminder</h3>
                <button
                  onClick={() => setShowReminderForm(false)}
                  className="text-gray-400 hover:text-gray-600"
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

                {/* NEW: Timezone Selection */}
                <div>
                  <label className="block text-sm font-medium text-gray-900">
                    <div className="flex items-center space-x-2">
                      <GlobeAltIcon className="h-4 w-4 text-gray-500" />
                      <span>Timezone</span>
                    </div>
                  </label>
                  <select
                    {...register('timezone', { required: 'Timezone is required' })}
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  >
                    {TIMEZONES.map((tz) => (
                      <option key={tz.value} value={tz.value}>
                        {tz.label}
                      </option>
                    ))}
                  </select>
                  {errors.timezone && (
                    <p className="mt-1 text-sm text-red-600">{errors.timezone.message}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">
                    Your detected timezone: {defaultTimezone}
                  </p>
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
                    className="text-gray-900 mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
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
                    className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
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