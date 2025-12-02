/* eslint-disable react/no-unescaped-entities */
'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api';
import { FeedbackType } from '@/lib/types';
import toast from 'react-hot-toast';

export default function FeedbackPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    type: FeedbackType.GENERAL,
    rating: 10,
    message: '',
  });

  // Fetch user data on component mount
  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const userData = await apiClient.getCurrentUser();
        setFormData(prev => ({
          ...prev,
          name: `${userData.firstName} ${userData.lastName}`,
          email: userData.email,
        }));
      } catch (error) {
        console.error('Failed to fetch user data:', error);
        toast.error('Failed to load user information');
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      await apiClient.submitFeedback(formData);
      toast.success('Thank you for your feedback!');
      // Reset only the fields that can be changed
      setFormData(prev => ({
        ...prev,
        type: FeedbackType.GENERAL,
        rating: 10,
        message: '',
      }));
      setTimeout(() => router.push('/dashboard'), 2000);
    } catch (error) {
      console.error('Failed to submit feedback:', error);
      toast.error('Failed to submit feedback. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'rating' ? parseInt(value) : value,
    }));
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white shadow-md rounded-lg p-8">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900">Send Feedback</h1>
            <p className="mt-2 text-gray-800">
              We'd love to hear your thoughts, suggestions, or issues you're experiencing.
            </p>
          </div>

          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
            {/* Name */}
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                Your Name
              </label>
              <input
                type="text"
                id="name"
                name="name"
                required
                value={formData.name}
                readOnly
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 bg-gray-50 text-gray-800 cursor-not-allowed shadow-sm focus:outline-none"
                placeholder="Loading..."
              />
              <p className="mt-1 text-xs text-gray-800">Auto-filled from your profile</p>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                Email Address
              </label>
              <input
                type="email"
                id="email"
                name="email"
                required
                value={formData.email}
                readOnly
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 bg-gray-50 text-gray-800 cursor-not-allowed shadow-sm focus:outline-none"
                placeholder="Loading..."
              />
              <p className="mt-1 text-xs text-gray-800">Auto-filled from your profile</p>
            </div>

            {/* Feedback Type */}
            <div>
              <label htmlFor="type" className="block text-sm font-medium text-gray-800">
                Feedback Type *
              </label>
              <select
                id="type"
                name="type"
                required
                value={formData.type}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm text-gray-800 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option value={FeedbackType.GENERAL}>General Feedback</option>
                <option value={FeedbackType.BUG}>Bug Report</option>
                <option value={FeedbackType.FEATURE_REQUEST}>Feature Request</option>
                <option value={FeedbackType.IMPROVEMENT}>Improvement Suggestion</option>
              </select>
            </div>

            {/* Recommendation Score */}
            <div>
              <label htmlFor="rating" className="block text-sm font-medium text-gray-800 mb-3">
                How likely are you to recommend CareerTrackr? *
              </label>
              <div className="space-y-4">
                {/* Number scale display */}
                <div className="flex justify-between items-center px-1">
                  {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setFormData(prev => ({ ...prev, rating: num }))}
                      className={`w-8 h-8 rounded-full text-sm font-semibold transition-all ${
                        formData.rating === num
                          ? 'bg-blue-600 text-white scale-110 shadow-lg'
                          : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                      }`}
                    >
                      {num}
                    </button>
                  ))}
                </div>
                
                {/* Slider */}
                <input
                  type="range"
                  id="rating"
                  name="rating"
                  min="0"
                  max="10"
                  value={formData.rating}
                  onChange={handleChange}
                  className="w-full h-2 bg-gradient-to-r from-red-200 via-yellow-200 to-green-200 rounded-lg appearance-none cursor-pointer slider"
                  style={{
                    background: `linear-gradient(to right, 
                      rgb(254 202 202) 0%, 
                      rgb(253 224 71) 50%, 
                      rgb(187 247 208) 100%)`
                  }}
                />
                
                {/* Labels */}
                <div className="flex justify-between text-xs text-gray-800 px-1">
                  <span className="font-medium">Not likely</span>
                  <span className="font-semibold text-lg text-blue-600">{formData.rating}</span>
                  <span className="font-medium">Very likely</span>
                </div>
              </div>
            </div>

            {/* Message */}
            <div>
              <label htmlFor="message" className="block text-sm font-medium text-gray-800">
                Your Message *
              </label>
              <textarea
                id="message"
                name="message"
                required
                rows={6}
                value={formData.message}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border text-gray-800 border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                placeholder="Tell us what's on your mind..."
              />
              <p className="mt-1 text-sm text-gray-800">
                Minimum 10 characters. Be as detailed as possible.
              </p>
            </div>

            {/* Submit Button */}
            <div className="flex items-center justify-between">
              <button
                type="button"
                onClick={() => router.back()}
                className="px-4 py-2 text-sm font-medium text-gray-800 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-6 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'Submitting...' : 'Submit Feedback'}
              </button>
            </div>
          </form>
          )}
        </div>

        {/* Info Section */}
        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold text-blue-900 mb-2">What happens next?</h2>
          <ul className="space-y-2 text-sm text-blue-800">
            <li className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              We'll review your feedback within 24-48 hours
            </li>
            <li className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              For bugs, we'll prioritize based on severity
            </li>
            <li className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              Feature requests will be considered for future updates
            </li>
            <li className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              You may receive a response via email
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}