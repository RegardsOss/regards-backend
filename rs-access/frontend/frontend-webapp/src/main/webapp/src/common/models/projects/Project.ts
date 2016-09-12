import { HateoasLink } from "../hateoas/types"

export interface Project {
  projectId: string,
  description: string
  isPublic: boolean
  icon: string
  name: string
  links: Array<HateoasLink>
}

export interface ProjectsStore {
  isFetching: boolean
  items: Array<Project>
  lastUpdate: string
}