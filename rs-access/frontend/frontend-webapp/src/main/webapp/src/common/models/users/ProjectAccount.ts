import { HateosLink } from '../hateos/types'
import { Account } from './Account'

export interface ProjectAccount {
  projectAccountId: number,
  status: number,
  lastconnection: string,
  lastupdate: string,
  role: string,
  project: string,
  account: Account,
  links: Array<HateosLink>
}
