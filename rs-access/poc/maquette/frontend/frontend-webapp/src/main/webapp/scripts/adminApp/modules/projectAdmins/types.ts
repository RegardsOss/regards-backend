type Link = {
  rel:string,
  href:string
}

export type ProjectAdmin = {
  name:string
  links?:Array<Link>
}
