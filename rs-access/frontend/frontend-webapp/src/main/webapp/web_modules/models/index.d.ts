export interface Project {
  projectId: string
  name: string
  description: string
  icon: string
  isPublic: boolean
  links: Array<HateoasLink>
}

export interface HateoasLink {
  rel: string,
  href: string
}

export interface Account {
  accountId: number,
  firstName: string,
  lastName: string,
  login: string,
  password?: string,
  status: string,
  email: string,
  links: Array<HateoasLink>
}
export interface ApiResultItems {
  entities: any,
  results: Array<string|number>
}

/**
 * Normalized action from api result
 */
export interface NormalizedAction {
  type: string,
  payload: ApiResultItems
}

export interface ApiStateResult<T> {
  isFetching: boolean,
  items: Array<T>,
  ids: Array<string>,
  lastUpdate: string
}

export interface ProjectAccount {
  projectAccountId: number,
  status: number,
  lastconnection: string,
  lastupdate: string,
  role: string,
  project: string,
  account: Account,
  links: Array<HateoasLink>
}

export interface ResourceAccess {
  description: string,
  microservice: string,
  resource: string,
  verb: string
  links: Array<HateoasLink>
}

export interface Role {
  name: string,
  parentRole: Role,
  permissions: Array<ResourceAccess>
  links: Array<HateoasLink>
}
