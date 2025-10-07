// src/components/JobCard.tsx - Reusable Job Card with Bookmark
import { useState } from 'react';
import { JobListing } from '@/lib/types';
import { formatSalary, timeAgo } from '@/lib/utils';
import Link from 'next/link';
import {
  BookmarkIcon,
  ArrowTopRightOnSquareIcon,
  BriefcaseIcon,
  MapPinIcon,
  CurrencyDollarIcon,
  CalendarIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';
import { BookmarkIcon as BookmarkIconSolid } from '@heroicons/react/24/solid';

interface JobCardProps {
  job: JobListing;
  isSaved: boolean;
  isApplied: boolean;
  onSave: (job: JobListing) => void;
  onMarkApplied: (job: JobListing) => void;
  isSaving?: boolean;
  isMarking?: boolean;
}

export const JobCard = ({
  job,
  isSaved,
  isApplied,
  onSave,
  onMarkApplied,
  isSaving = false,
  isMarking = false,
}: JobCardProps) => {
  const [isHovered, setIsHovered] = useState(false);

  return (
    <div
      className="border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-all duration-200 relative bg-white"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Bookmark Button - ENHANCED with color feedback */}
      <button
        onClick={(e) => {
          e.preventDefault();
          onSave(job);
        }}
        disabled={isSaving}
        className={`absolute top-4 right-4 p-2 rounded-full transition-all duration-200 ${
          isSaved
            ? 'text-yellow-500 bg-yellow-50 hover:bg-yellow-100'
            : 'text-gray-400 hover:text-yellow-500 hover:bg-yellow-50'
        } ${isSaving ? 'opacity-50 cursor-not-allowed' : ''}`}
        title={isSaved ? 'Remove from saved' : 'Save job'}
      >
        {isSaved ? (
          <BookmarkIconSolid className="h-6 w-6 drop-shadow-sm" />
        ) : (
          <BookmarkIcon className={`h-6 w-6 ${isHovered ? 'animate-pulse' : ''}`} />
        )}
      </button>

      {/* Applied Badge */}
      {isApplied && (
        <div className="absolute top-4 left-4">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 border border-green-200">
            <CheckCircleIcon className="w-3 h-3 mr-1" />
            Applied
          </span>
        </div>
      )}

      <div className="pr-8 pt-8">
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
                  className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-indigo-50 text-indigo-700 border border-indigo-200"
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
          {job.applyUrl && (
            <a
              href={job.applyUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 shadow-sm transition-colors"
            >
              <ArrowTopRightOnSquareIcon className="h-4 w-4 mr-2" />
              Apply on Company Site
            </a>
          )}

          <button
            onClick={() => onMarkApplied(job)}
            disabled={isMarking || isApplied}
            className={`inline-flex items-center justify-center px-4 py-2 text-sm font-medium rounded-md shadow-sm transition-all duration-200 ${
              isApplied
                ? 'text-green-700 bg-green-100 cursor-not-allowed border border-green-300'
                : 'text-white bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 border border-transparent'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
          >
            {isMarking ? (
              <>
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Marking...
              </>
            ) : isApplied ? (
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
  );
};