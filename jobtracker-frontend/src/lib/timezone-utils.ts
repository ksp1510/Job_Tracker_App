// src/lib/timezone-utils.ts
// Utility functions for timezone handling

/**
 * Convert date, time, and timezone to ISO 8601 format with timezone offset
 * @param date - Date string in YYYY-MM-DD format
 * @param time - Time string in HH:mm format
 * @param timezone - IANA timezone identifier (e.g., "America/New_York")
 * @returns ISO 8601 datetime string with timezone offset (e.g., "2025-10-28T21:50:00-04:00")
 */
export function toISO8601WithTimezone(
  date: string,
  time: string,
  timezone: string
): string {
  // Combine date and time into a datetime string
  const dateTimeString = `${date}T${time}:00`;

  // Create a Date object
  const dateObj = new Date(dateTimeString);

  // Format the date in the specified timezone
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });

  const parts = formatter.formatToParts(dateObj);
  const getPart = (type: string) =>
    parts.find((p) => p.type === type)?.value || '';

  const year = getPart('year');
  const month = getPart('month');
  const day = getPart('day');
  const hour = getPart('hour');
  const minute = getPart('minute');
  const second = getPart('second');

  // Reconstruct the datetime in the target timezone
  const localDateTimeString = `${year}-${month}-${day}T${hour}:${minute}:${second}`;

  // Get timezone offset
  const offset = getTimezoneOffset(timezone, new Date(localDateTimeString));

  return `${date}T${time}:00${offset}`;
}

/**
 * Get timezone offset in ISO 8601 format (+HH:mm or -HH:mm)
 * @param timezone - IANA timezone identifier
 * @param date - Date to calculate offset for (handles DST)
 * @returns Offset string (e.g., "-04:00", "+05:30")
 */
function getTimezoneOffset(timezone: string, date: Date): string {
  // Get the date string in both the target timezone and UTC
  const targetTime = new Date(
    date.toLocaleString('en-US', { timeZone: timezone })
  ).getTime();
  const utcTime = new Date(
    date.toLocaleString('en-US', { timeZone: 'UTC' })
  ).getTime();

  // Calculate offset in minutes
  const offsetMinutes = (targetTime - utcTime) / (1000 * 60);

  // Convert to hours and minutes
  const offsetHours = Math.floor(Math.abs(offsetMinutes) / 60);
  const offsetMins = Math.abs(offsetMinutes) % 60;

  // Format as +HH:mm or -HH:mm
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const hours = String(offsetHours).padStart(2, '0');
  const minutes = String(offsetMins).padStart(2, '0');

  return `${sign}${hours}:${minutes}`;
}

/**
 * Format UTC datetime string to local time with timezone name
 * @param utcString - UTC datetime string (without Z suffix)
 * @param options - Formatting options
 * @returns Formatted local datetime string
 */
export function formatUtcToLocal(
  utcString: string,
  options?: {
    showTimezone?: boolean;
    format24Hour?: boolean;
  }
): string {
  try {
    // Add Z to indicate UTC
    const date = new Date(utcString + 'Z');
    const userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

    const formatOptions: Intl.DateTimeFormatOptions = {
      timeZone: userTimezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: !options?.format24Hour,
    };

    if (options?.showTimezone) {
      formatOptions.timeZoneName = 'short';
    }

    return date.toLocaleString('en-CA', formatOptions);
  } catch (error) {
    console.error('Error formatting date:', error);
    return utcString;
  }
}

/**
 * Get current user's timezone
 * @returns IANA timezone identifier
 */
export function getUserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * Common timezones for dropdown menus
 */
export const COMMON_TIMEZONES = [
  // North America
  { value: 'America/New_York', label: 'Eastern Time (ET)', region: 'North America' },
  { value: 'America/Chicago', label: 'Central Time (CT)', region: 'North America' },
  { value: 'America/Denver', label: 'Mountain Time (MT)', region: 'North America' },
  { value: 'America/Los_Angeles', label: 'Pacific Time (PT)', region: 'North America' },
  { value: 'America/Anchorage', label: 'Alaska Time (AKT)', region: 'North America' },
  { value: 'Pacific/Honolulu', label: 'Hawaii Time (HT)', region: 'North America' },
  { value: 'America/Phoenix', label: 'Arizona Time (MST)', region: 'North America' },
  
  // Canada
  { value: 'America/Toronto', label: 'Toronto (ET)', region: 'Canada' },
  { value: 'America/Vancouver', label: 'Vancouver (PT)', region: 'Canada' },
  { value: 'America/Edmonton', label: 'Edmonton (MT)', region: 'Canada' },
  { value: 'America/Winnipeg', label: 'Winnipeg (CT)', region: 'Canada' },
  { value: 'America/Halifax', label: 'Halifax (AT)', region: 'Canada' },
  { value: 'America/St_Johns', label: 'Newfoundland (NT)', region: 'Canada' },
  
  // Europe
  { value: 'Europe/London', label: 'London (GMT/BST)', region: 'Europe' },
  { value: 'Europe/Paris', label: 'Paris (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Berlin', label: 'Berlin (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Madrid', label: 'Madrid (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Rome', label: 'Rome (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Amsterdam', label: 'Amsterdam (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Stockholm', label: 'Stockholm (CET/CEST)', region: 'Europe' },
  { value: 'Europe/Moscow', label: 'Moscow (MSK)', region: 'Europe' },
  
  // Asia
  { value: 'Asia/Tokyo', label: 'Tokyo (JST)', region: 'Asia' },
  { value: 'Asia/Shanghai', label: 'Shanghai (CST)', region: 'Asia' },
  { value: 'Asia/Hong_Kong', label: 'Hong Kong (HKT)', region: 'Asia' },
  { value: 'Asia/Singapore', label: 'Singapore (SGT)', region: 'Asia' },
  { value: 'Asia/Dubai', label: 'Dubai (GST)', region: 'Asia' },
  { value: 'Asia/Kolkata', label: 'India (IST)', region: 'Asia' },
  { value: 'Asia/Bangkok', label: 'Bangkok (ICT)', region: 'Asia' },
  { value: 'Asia/Seoul', label: 'Seoul (KST)', region: 'Asia' },
  
  // Australia & Pacific
  { value: 'Australia/Sydney', label: 'Sydney (AEDT/AEST)', region: 'Australia' },
  { value: 'Australia/Melbourne', label: 'Melbourne (AEDT/AEST)', region: 'Australia' },
  { value: 'Australia/Perth', label: 'Perth (AWST)', region: 'Australia' },
  { value: 'Pacific/Auckland', label: 'Auckland (NZDT/NZST)', region: 'Pacific' },
  
  // South America
  { value: 'America/Sao_Paulo', label: 'São Paulo (BRT)', region: 'South America' },
  { value: 'America/Buenos_Aires', label: 'Buenos Aires (ART)', region: 'South America' },
  { value: 'America/Santiago', label: 'Santiago (CLT)', region: 'South America' },
  
  // Africa
  { value: 'Africa/Cairo', label: 'Cairo (EET)', region: 'Africa' },
  { value: 'Africa/Johannesburg', label: 'Johannesburg (SAST)', region: 'Africa' },
  { value: 'Africa/Lagos', label: 'Lagos (WAT)', region: 'Africa' },
  
  // UTC
  { value: 'UTC', label: 'UTC (Coordinated Universal Time)', region: 'UTC' },
];

/**
 * Validate that a datetime is in the future
 * @param date - Date string
 * @param time - Time string
 * @param timezone - Timezone identifier
 * @returns true if datetime is in the future
 */
export function isFutureDateTime(
  date: string,
  time: string,
  timezone: string
): boolean {
  try {
    const dateTimeStr = toISO8601WithTimezone(date, time, timezone);
    const dateTime = new Date(dateTimeStr);
    return dateTime.getTime() > Date.now();
  } catch {
    return false;
  }
}

/**
 * Get human-readable relative time
 * @param utcString - UTC datetime string
 * @returns Relative time string (e.g., "in 2 hours", "yesterday")
 */
export function getRelativeTime(utcString: string): string {
  try {
    const date = new Date(utcString + 'Z');
    const now = new Date();
    const diffMs = date.getTime() - now.getTime();
    const diffMins = Math.round(diffMs / (1000 * 60));
    const diffHours = Math.round(diffMs / (1000 * 60 * 60));
    const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 60 && diffMins > -60) {
      if (diffMins === 0) return 'now';
      if (diffMins > 0) return `in ${diffMins} minute${diffMins !== 1 ? 's' : ''}`;
      return `${Math.abs(diffMins)} minute${diffMins !== -1 ? 's' : ''} ago`;
    }

    if (diffHours < 24 && diffHours > -24) {
      if (diffHours > 0) return `in ${diffHours} hour${diffHours !== 1 ? 's' : ''}`;
      return `${Math.abs(diffHours)} hour${diffHours !== -1 ? 's' : ''} ago`;
    }

    if (diffDays > 0) {
      if (diffDays === 1) return 'tomorrow';
      if (diffDays < 7) return `in ${diffDays} days`;
      const weeks = Math.floor(diffDays / 7);
      return `in ${weeks} week${weeks !== 1 ? 's' : ''}`;
    } else {
      if (diffDays === -1) return 'yesterday';
      if (diffDays > -7) return `${Math.abs(diffDays)} days ago`;
      const weeks = Math.floor(Math.abs(diffDays) / 7);
      return `${weeks} week${weeks !== 1 ? 's' : ''} ago`;
    }
  } catch {
    return '';
  }
}