

type AccessRightsView = {
  name:string,
  access:boolean
}

type Dependencies = {
  GET: Array<string>,
  POST: Array<string>,
  PUT: Array<string>,
  DELETE: Array<string>
}
