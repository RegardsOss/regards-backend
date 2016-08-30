import { HateosLink } from "../hateos/types"

export interface ProjectAccount {
  projectAccountId: number,
  status: number,
  lastconnection: string,
  lastupdate: string,
  role: string,
  project: string,
  account: number,
  links: Array<HateosLink>
}
