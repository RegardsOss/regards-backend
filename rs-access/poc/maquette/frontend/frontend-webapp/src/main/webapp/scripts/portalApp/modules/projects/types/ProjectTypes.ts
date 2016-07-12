
export type Project = {
  name: string
}

export type ProjectsStore = {
  isFetching: boolean,
  items: Array<Project>,
  lastUpdate: string
}
