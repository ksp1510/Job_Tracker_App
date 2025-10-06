// src/app/settings/page.tsx
'use client';

import { useAuth } from '@/context/AuthContext';
import { Navbar } from '@/components/layout/Navbar';
import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  UserCircleIcon,
  BellIcon,
  EnvelopeIcon,
  DevicePhoneMobileIcon,
} from '@heroicons/react/24/outline';

export default function SettingsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [inAppNotifications, setInAppNotifications] = useState(true);

  // Fetch current user preferences
  const { data: currentUser } = useQuery({
    queryKey: ['currentUser'],
    queryFn: () => apiClient.getCurrentUser(),
  });

  // Set initial values from user data
  useEffect(() => {
    if (currentUser) {
      setNotificationsEnabled(currentUser.notificationEnabled);
      setEmailNotifications(currentUser.emailNotificationsEnabled);
      setInAppNotifications(currentUser.inAppNotificationsEnabled);
    }
  }, [currentUser]);

  // Save preferences mutation
  const savePreferencesMutation = useMutation({
    mutationFn: async () => {
      // This would call a backend endpoint to update preferences
      // For now, we'll just show success
      return Promise.resolve();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
      toast.success('Preferences saved successfully');
    },
    onError: () => {
      toast.error('Failed to save preferences');
    },
  });

  const handleSavePreferences = () => {
    savePreferencesMutation.mutate();
  };

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-4xl mx-auto py-6 sm:px-6 lg:px-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Settings</h1>

        {/* Profile Section */}
        <div className="bg-white shadow rounded-lg mb-6 p-6">
          <div className="flex items-center mb-4">
            <UserCircleIcon className="h-6 w-6 text-gray-700 mr-2" />
            <h2 className="text-lg font-medium text-gray-900">Profile Information</h2>
          </div>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">First Name</label>
                <p className="mt-1 text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md">{user.firstName}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Last Name</label>
                <p className="mt-1 text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md">{user.lastName}</p>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Email</label>
              <p className="mt-1 text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md">{user.email}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Role</label>
              <p className="mt-1 text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md capitalize">{user.role.toLowerCase()}</p>
            </div>
          </div>
        </div>

        {/* Notification Preferences */}
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex items-center mb-4">
            <BellIcon className="h-6 w-6 text-gray-700 mr-2" />
            <h2 className="text-lg font-medium text-gray-900">Notification Preferences</h2>
          </div>
          <div className="space-y-4">
            <div className="flex items-center justify-between py-3 border-b border-gray-200">
              <div className="flex items-center">
                <BellIcon className="h-5 w-5 text-gray-500 mr-3" />
                <div>
                  <p className="text-sm font-medium text-gray-900">Enable Notifications</p>
                  <p className="text-xs text-gray-500">Receive all types of notifications</p>
                </div>
              </div>
              <button
                onClick={() => setNotificationsEnabled(!notificationsEnabled)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  notificationsEnabled ? 'bg-indigo-600' : 'bg-gray-200'
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    notificationsEnabled ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-200">
              <div className="flex items-center">
                <EnvelopeIcon className="h-5 w-5 text-gray-500 mr-3" />
                <div>
                  <p className="text-sm font-medium text-gray-900">Email Notifications</p>
                  <p className="text-xs text-gray-500">Receive notifications via email</p>
                </div>
              </div>
              <button
                onClick={() => setEmailNotifications(!emailNotifications)}
                disabled={!notificationsEnabled}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  emailNotifications && notificationsEnabled ? 'bg-indigo-600' : 'bg-gray-200'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    emailNotifications && notificationsEnabled ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
            
            <div className="flex items-center justify-between py-3">
              <div className="flex items-center">
                <DevicePhoneMobileIcon className="h-5 w-5 text-gray-500 mr-3" />
                <div>
                  <p className="text-sm font-medium text-gray-900">In-App Notifications</p>
                  <p className="text-xs text-gray-500">Receive notifications in the application</p>
                </div>
              </div>
              <button
                onClick={() => setInAppNotifications(!inAppNotifications)}
                disabled={!notificationsEnabled}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  inAppNotifications && notificationsEnabled ? 'bg-indigo-600' : 'bg-gray-200'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    inAppNotifications && notificationsEnabled ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
          </div>
          
          <div className="mt-6 pt-6 border-t border-gray-200">
            <button
              onClick={handleSavePreferences}
              disabled={savePreferencesMutation.isPending}
              className="w-full sm:w-auto px-6 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {savePreferencesMutation.isPending ? 'Saving...' : 'Save Preferences'}
            </button>
          </div>
        </div>

        {/* Account Actions */}
        <div className="bg-white shadow rounded-lg p-6 mt-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Account Actions</h2>
          <div className="space-y-3">
            <button
              className="w-full text-left px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Change Password
              </button>
              <button
                className="w-full text-left px-4 py-2 border border-red-300 rounded-md text-sm font-medium text-red-600 hover:bg-red-50"
              >
                Delete Account
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }