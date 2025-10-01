/* eslint-disable @typescript-eslint/no-explicit-any */
/* src/app/applications/page.tsx */
'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Navbar } from '@/components/layout/Navbar';
import { apiClient } from '@/lib/api';
import { Application, ApplicationStatus } from '@/lib/types';
import { getStatusColor, formatDate } from '@/lib/utils';

export default function ApplicationsListPage() {
  // Fetch all applications via the API
  const { data: applications = [], isLoading } = useQuery({
    queryKey: ['applications'],
    queryFn: () => apiClient.getApplications(),
  });

  // Local state for a status filter (All, Applied, Interview, etc.)
  const [statusFilter, setStatusFilter] = useState<'ALL' | ApplicationStatus>('ALL');
  const filteredApps =
    statusFilter === 'ALL'
      ? applications
      : applications.filter((app) => app.status === statusFilter);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Page header */}
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Applications</h1>

        {/* Status filter */}
        <div className="mb-4">
          <label htmlFor="statusFilter" className="mr-2 text-sm font-medium text-gray-700">
            Filter by status:
          </label>
          <select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as any)}
            className="border border-gray-300 rounded-md px-2 py-1 text-sm"
          >
            <option value="ALL">All</option>
            {Object.values(ApplicationStatus).map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </div>

        {/* Table / card list */}
        {isLoading ? (
          <p>Loading…</p>
        ) : filteredApps.length === 0 ? (
          <p>No applications found.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full bg-white rounded-md overflow-hidden">
              <thead className="bg-gray-100 text-gray-600 uppercase text-sm">
                <tr>
                  <th className="py-3 px-4 text-left">Company</th>
                  <th className="py-3 px-4 text-left">Job Title</th>
                  <th className="py-3 px-4 text-left">Applied Date</th>
                  <th className="py-3 px-4 text-left">Status</th>
                  <th className="py-3 px-4 text-left"></th>
                </tr>
              </thead>
              <tbody className="text-gray-700 text-sm">
                {filteredApps.map((app) => (
                  <tr key={app.id} className="border-b border-gray-200 hover:bg-gray-50">
                    <td className="py-3 px-4">{app.companyName}</td>
                    <td className="py-3 px-4">{app.jobTitle}</td>
                    <td className="py-3 px-4">
                      {app.appliedDate ? formatDate(app.appliedDate) : '-'}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                          app.status,
                        )}`}
                      >
                        {app.status}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <Link
                        href={`/applications/${app.id}`}
                        className="text-indigo-600 hover:text-indigo-800"
                      >
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
