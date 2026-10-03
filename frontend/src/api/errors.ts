import { ApiError } from './client';
import type { ApiProblem, FieldErrorItem } from './types';

export function problemOf(error: unknown): ApiProblem | null {
  return error instanceof ApiError ? error.problem : null;
}

export function fieldErrorsOf(error: unknown): FieldErrorItem[] {
  return problemOf(error)?.fieldErrors ?? [];
}

export function fieldErrorFor(error: unknown, field: string): string | undefined {
  return fieldErrorsOf(error).find((e) => e.field === field)?.message;
}

export function isProblem(error: unknown, code: string): boolean {
  return problemOf(error)?.code === code;
}
