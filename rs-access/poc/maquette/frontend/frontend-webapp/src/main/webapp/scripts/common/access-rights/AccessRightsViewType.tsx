

export type AccessRightsView = {
  name:string,
  access:boolean
}

export type Dependencies = {
  GET?: Array<string>,
  POST?: Array<string>,
  PUT?: Array<string>,
  DELETE?: Array<string>
}
