import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * Combines multiple class names into a single string, merging Tailwind CSS classes.
 * This is a utility function used by shadcn/ui components.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Formats a date using the Intl.DateTimeFormat API.
 * @param date The date to format
 * @param options The options for formatting
 * @returns The formatted date string
 */
export function formatDate(
  date: Date | number,
  options: Intl.DateTimeFormatOptions = {
    day: "numeric",
    month: "short",
    year: "numeric",
  }
) {
  return new Intl.DateTimeFormat("en-US", {
    ...options,
  }).format(date)
}

/**
 * Formats a timestamp in milliseconds to a human-readable string.
 * @param timestamp The timestamp in milliseconds
 * @returns The formatted timestamp string
 */
export function formatTimestamp(timestamp: number) {
  return formatDate(timestamp, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "numeric",
    second: "numeric",
  })
}

/**
 * Truncates a string to a specified length and adds an ellipsis if needed.
 * @param str The string to truncate
 * @param length The maximum length of the string
 * @returns The truncated string
 */
export function truncate(str: string, length: number) {
  if (!str) return ""
  return str.length > length ? `${str.substring(0, length)}...` : str
}

/**
 * Delays execution for a specified number of milliseconds.
 * @param ms The number of milliseconds to delay
 * @returns A promise that resolves after the specified delay
 */
export function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}