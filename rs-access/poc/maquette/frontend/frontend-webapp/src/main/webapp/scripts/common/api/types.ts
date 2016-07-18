import { Action } from 'flux-standard-action'

interface FsaError extends Error {
  response: any,
  status: number,
  statusText: string
}

export interface FsaErrorAction extends Action<FsaError> {
    error: boolean;
}

export const FsaErrorDefault: FsaError = {
  message: '500 - Internal Server Error',
  name: 'ApiError',
  response: undefined,
  status: 500,
  statusText: 'Internal Server Error'
}
